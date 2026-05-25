package org.dsb.fundvaluation.service;

import org.dsb.fundvaluation.model.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);
    private static final Pattern QUOTE_PATTERN = Pattern.compile("(v_[a-z0-9_]+=\".*?\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern SYMBOL_BODY_PATTERN = Pattern.compile(
            "v_(?<symbol>[a-z0-9_]+)=\"(?<body>.*)\"", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;

    public QuoteService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Map<String, Quote> fetchQuotes(List<String> symbols) throws IOException, InterruptedException {
        if (symbols.isEmpty()) return Collections.emptyMap();

        String query = String.join(",", symbols);
        String url = "https://qt.gtimg.cn/q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String text = response.body();

        Map<String, Quote> quotes = new LinkedHashMap<>();
        var matcher = QUOTE_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                Quote quote = parseQuoteLine(matcher.group(1));
                quotes.put(quote.getSymbol(), quote);
            } catch (Exception e) {
                log.warn("Failed to parse quote line: {}", e.getMessage());
            }
        }
        return quotes;
    }

    private Quote parseQuoteLine(String line) {
        var matcher = SYMBOL_BODY_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unparseable quote line: " + line);
        }
        String symbol = matcher.group("symbol");
        String body = matcher.group("body");
        String[] parts = body.split("~");

        String name = parts.length > 1 ? parts[1] : "";
        BigDecimal lastPrice = parts.length > 3 ? new BigDecimal(parts[3]) : BigDecimal.ZERO;
        BigDecimal prevClose = parts.length > 4 ? new BigDecimal(parts[4]) : BigDecimal.ZERO;

        return new Quote(symbol, name, lastPrice, prevClose);
    }
}