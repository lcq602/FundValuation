package org.dsb.fundvaluation.service;

import org.dsb.fundvaluation.dto.NewsItem;
import org.dsb.fundvaluation.dto.NewsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class NewsService {
    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final String DEFAULT_QUERY = "AI 半导体 PCB 光模块";
    private static final String CACHE_KEY = "news:roll:eastmoney";
    private static final String ROLL_URL = "https://roll.eastmoney.com/";
    private static final Pattern LINK_PATTERN = Pattern.compile(
            "<a[^>]+href=\"(?<url>[^\"]+)\"[^>]*>(?<title>.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final RestTemplate restTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public NewsService(RestTemplate restTemplate, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this(restTemplate, redisTemplate, objectMapper, Clock.systemUTC());
    }

    public NewsService(RestTemplate restTemplate, StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper, Clock clock) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public NewsResponse search(String query, int limit) {
        String normalizedQuery = normalizeQuery(query);
        int normalizedLimit = Math.max(1, Math.min(limit, 30));
        CachedNews cachedNews = readCache();

        if (cachedNews.items().isEmpty()) {
            cachedNews = refreshCache();
        }

        NewsResponse response = new NewsResponse();
        response.setQuery(normalizedQuery);
        response.setGeneratedAt(cachedNews.generatedAt());
        response.setItems(filter(cachedNews.items(), normalizedQuery, normalizedLimit));
        response.setError(cachedNews.error());
        return response;
    }

    @Scheduled(fixedDelayString = "${news.refresh.interval-ms:3600000}", initialDelayString = "${news.refresh.initial-delay-ms:5000}")
    public void scheduledRefresh() {
        refreshCache();
    }

    CachedNews refreshCache() {
        try {
            List<NewsItem> items = fetchRollNews();
            double generatedAt = Instant.now(clock).getEpochSecond();
            var cachedNews = new CachedNews(generatedAt, items, "");
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(cachedNews));
            log.info("News cache refreshed with {} items", items.size());
            return cachedNews;
        } catch (Exception e) {
            log.warn("News refresh failed: {}", e.getMessage());
            return new CachedNews(Instant.now(clock).getEpochSecond(), List.of(), "实时资讯获取失败，请稍后重试");
        }
    }

    List<NewsItem> parseRollNews(String html) {
        List<NewsItem> items = new ArrayList<>();
        var matcher = LINK_PATTERN.matcher(html);
        LinkedHashSet<String> seenUrls = new LinkedHashSet<>();

        while (matcher.find()) {
            String title = cleanText(matcher.group("title"));
            String url = normalizeUrl(matcher.group("url"));

            if (!isNewsUrl(url) || title.length() < 6 || !seenUrls.add(url)) {
                continue;
            }

            NewsItem item = new NewsItem();
            item.setTitle(title);
            item.setUrl(url);
            item.setSource("东方财富");
            item.setCategory(categoryFromUrl(url));
            item.setPublishedAt("");
            items.add(item);
        }

        return items;
    }

    private CachedNews readCache() {
        try {
            String payload = redisTemplate.opsForValue().get(CACHE_KEY);
            if (payload == null || payload.isBlank()) {
                return new CachedNews(0, List.of(), "");
            }
            return objectMapper.readValue(payload, new TypeReference<CachedNews>() {});
        } catch (Exception e) {
            log.warn("Failed to read news cache: {}", e.getMessage());
            return new CachedNews(0, List.of(), "");
        }
    }

    private List<NewsItem> fetchRollNews() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");
        headers.set(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        try {
            var entity = new HttpEntity<Void>(headers);
            var response = restTemplate.exchange(ROLL_URL, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("HTTP " + response.getStatusCode().value());
            }
            return parseRollNews(response.getBody() == null ? "" : response.getBody());
        } catch (RestClientException e) {
            throw new IllegalStateException("External news API request failed", e);
        }
    }

    private List<NewsItem> filter(List<NewsItem> items, String query, int limit) {
        List<String> terms = terms(query);
        List<NewsItem> matched = items.stream()
                .filter(item -> matches(item.getTitle(), terms))
                .limit(limit)
                .toList();
        if (!matched.isEmpty()) {
            return matched;
        }
        return items.stream().limit(limit).toList();
    }

    private boolean matches(String title, List<String> terms) {
        String lowerTitle = title.toLowerCase();
        return terms.stream().anyMatch(term -> lowerTitle.contains(term.toLowerCase()));
    }

    private List<String> terms(String query) {
        return List.of(query.split("[\\s,，、]+")).stream()
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .toList();
    }

    private String normalizeQuery(String query) {
        return query == null || query.isBlank() ? DEFAULT_QUERY : query.trim();
    }

    private String cleanText(String value) {
        return TAG_PATTERN.matcher(value)
                .replaceAll("")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .trim();
    }

    private String normalizeUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("/")) {
            return "https://roll.eastmoney.com" + url;
        }
        return "https://roll.eastmoney.com/" + url;
    }

    private boolean isNewsUrl(String url) {
        return url.matches("https?://(finance|stock|futures|forex|fund|hk|bond|bank|insurance|money)\\.eastmoney\\.com/a/.*\\.html");
    }

    private String categoryFromUrl(String url) {
        if (url.contains("//stock.")) return "股票";
        if (url.contains("//finance.")) return "财经";
        if (url.contains("//futures.")) return "期货";
        if (url.contains("//fund.")) return "基金";
        if (url.contains("//hk.")) return "港美";
        return "资讯";
    }

    public static class CachedNews {
        private double generatedAt;
        private List<NewsItem> items;
        private String error;

        public CachedNews() {
            this(0, List.of(), "");
        }

        public CachedNews(double generatedAt, List<NewsItem> items, String error) {
            this.generatedAt = generatedAt;
            this.items = items;
            this.error = error;
        }

        public double generatedAt() {
            return generatedAt;
        }

        public List<NewsItem> items() {
            return items == null ? List.of() : items;
        }

        public String error() {
            return error == null ? "" : error;
        }

        public double getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(double generatedAt) {
            this.generatedAt = generatedAt;
        }

        public List<NewsItem> getItems() {
            return items;
        }

        public void setItems(List<NewsItem> items) {
            this.items = items;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
