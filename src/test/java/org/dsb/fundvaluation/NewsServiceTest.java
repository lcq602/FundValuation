package org.dsb.fundvaluation;

import org.dsb.fundvaluation.dto.NewsItem;
import org.dsb.fundvaluation.service.NewsService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NewsServiceTest {

    @Test
    void searchReadsItemsFromRedisCacheAndFiltersByQuery() {
        var redisTemplate = mockRedis("""
                {
                  "generatedAt": 1779696000,
                  "items": [
                    {"title":"AI 算力订单带动光模块景气","url":"https://stock.eastmoney.com/a/202605253748075277.html","source":"东方财富","category":"股票","published_at":""},
                    {"title":"消费板块午后拉升","url":"https://finance.eastmoney.com/a/202605253748075278.html","source":"东方财富","category":"财经","published_at":""}
                  ]
                }
                """);

        var service = new NewsService(new RestTemplate(), redisTemplate, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-05-25T12:00:00Z"), ZoneId.of("UTC")));

        var response = service.search("AI 光模块", 10);

        assertThat(response.getError()).isEmpty();
        assertThat(response.getGeneratedAt()).isEqualTo(1779696000);
        assertThat(response.getItems()).extracting(NewsItem::getTitle)
                .containsExactly("AI 算力订单带动光模块景气");
    }

    @Test
    void searchFetchesWithRestTemplateAndWritesRedisWhenCacheIsEmpty() {
        var restTemplate = new RestTemplate();
        var server = MockRestServiceServer.bindTo(restTemplate).build();
        var redisTemplate = mockRedis(null);
        var html = """
                <html><body>
                <a href="https://stock.eastmoney.com/a/202605253748075277.html">AI 算力订单带动光模块景气</a>
                <a href="https://finance.eastmoney.com/a/202605253748075278.html">半导体设备国产替代推进</a>
                </body></html>
                """;

        server.expect(requestTo("https://roll.eastmoney.com/"))
                .andExpect(header("User-Agent", "Mozilla/5.0"))
                .andRespond(withSuccess(html, new MediaType("text", "html", StandardCharsets.UTF_8)));

        var service = new NewsService(restTemplate, redisTemplate, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-05-25T12:00:00Z"), ZoneId.of("UTC")));

        var response = service.search("半导体", 10);

        assertThat(response.getError()).isEmpty();
        assertThat(response.getGeneratedAt()).isEqualTo(1779710400);
        assertThat(response.getItems()).extracting(NewsItem::getTitle)
                .containsExactly("半导体设备国产替代推进");
        verify(redisTemplate.opsForValue()).set(eq("news:roll:eastmoney"), contains("半导体设备国产替代推进"));
        server.verify();
    }

    private StringRedisTemplate mockRedis(String cachedPayload) {
        var redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(cachedPayload);
        return redisTemplate;
    }
}
