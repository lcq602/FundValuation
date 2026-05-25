package org.dsb.fundvaluation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

@Service
public class NavService {

    private static final Logger log = LoggerFactory.getLogger(NavService.class);
    private static final String USER_AGENT = "Mozilla/5.0";

    private static final Pattern[] NAV_PATTERNS = {
            Pattern.compile("fix_dwjz[^0-9]*([0-9]+\\.[0-9]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("单位净值[^0-9]*([0-9]+\\.[0-9]+)"),
            Pattern.compile("\"dwjz\"\\s*:\\s*\"?([0-9]+\\.[0-9]+)\"?"),
    };

    private static final Pattern[] DATE_PATTERNS = {
            Pattern.compile("fix_date[^\\d]*(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("截止至[^\\d]*(\\d{4}-\\d{2}-\\d{2})"),
            Pattern.compile("jzrq[^\\d]*(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE),
    };

    private final HttpClient httpClient;

    public NavService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public NavResult fetchLatestNav(String fundCode) throws IOException, InterruptedException {
        String url = "https://fund.eastmoney.com/" + fundCode + ".html";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .header("Referer", url)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String text = response.body();

        String navText = findFirstMatch(NAV_PATTERNS, text);
        if (navText.isEmpty()) {
            throw new IOException("Could not parse NAV for " + fundCode);
        }

        String dateText = findFirstMatch(DATE_PATTERNS, text);
        return new NavResult(new BigDecimal(navText), dateText);
    }

    private String findFirstMatch(Pattern[] patterns, String text) {
        for (Pattern pattern : patterns) {
            var matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }

    public record NavResult(BigDecimal nav, String date) {}
}