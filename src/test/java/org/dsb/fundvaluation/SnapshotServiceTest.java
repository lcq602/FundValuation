package org.dsb.fundvaluation;

import org.dsb.fundvaluation.service.FundDataService;
import org.dsb.fundvaluation.service.NavService;
import org.dsb.fundvaluation.service.QuoteService;
import org.dsb.fundvaluation.service.RetryScheduler;
import org.dsb.fundvaluation.service.SnapshotService;
import org.dsb.fundvaluation.model.FundData;
import org.dsb.fundvaluation.model.HoldingData;
import org.dsb.fundvaluation.model.Quote;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SnapshotServiceTest {

    @Test
    void getSnapshotReadsRedisCacheBeforeMemorySnapshot() {
        var redisTemplate = mockRedis("""
                {
                  "generatedAt": 1779696000,
                  "funds": [
                    {
                      "fundCode": "000001",
                      "fundName": "Cached Fund",
                      "estimatedNav": 1.23,
                      "estimatedChangePct": 0.45,
                      "updatedAt": "2026-05-25"
                    }
                  ],
                  "details": {}
                }
                """);
        var fundDataService = mock(FundDataService.class);
        when(fundDataService.getAllFunds()).thenReturn(List.of());

        var service = new SnapshotService(fundDataService, mock(NavService.class), mock(QuoteService.class),
                mock(RetryScheduler.class), redisTemplate, new ObjectMapper());

        var snapshot = service.getSnapshot();

        assertThat(snapshot.getGeneratedAt()).isEqualTo(1779696000);
        assertThat(snapshot.getFunds()).hasSize(1);
        assertThat(snapshot.getFunds().get(0).getFundName()).isEqualTo("Cached Fund");
    }

    @Test
    void initDoesNotWriteSkeletonSnapshotToRedis() {
        var redisTemplate = mockRedis(null);
        var fundDataService = mock(FundDataService.class);
        when(fundDataService.getAllFunds()).thenReturn(List.of());

        var service = new SnapshotService(fundDataService, mock(NavService.class), mock(QuoteService.class),
                mock(RetryScheduler.class), redisTemplate, new ObjectMapper());

        service.init();

        verify(redisTemplate.opsForValue(), never()).set(eq("fund:snapshot"), anyString());
    }

    @Test
    void getSnapshotIgnoresCachedSkeletonSnapshot() {
        var redisTemplate = mockRedis("""
                {
                  "generatedAt": 0,
                  "funds": [
                    {
                      "fundCode": "000001",
                      "fundName": "Skeleton Fund",
                      "estimatedNav": 0,
                      "estimatedChangePct": 0,
                      "updatedAt": ""
                    }
                  ],
                  "details": {}
                }
                """);
        var fundDataService = mock(FundDataService.class);
        when(fundDataService.getAllFunds()).thenReturn(List.of());

        var service = new SnapshotService(fundDataService, mock(NavService.class), mock(QuoteService.class),
                mock(RetryScheduler.class), redisTemplate, new ObjectMapper());

        var snapshot = service.getSnapshot();

        assertThat(snapshot.getGeneratedAt()).isEqualTo(0);
        assertThat(snapshot.getFunds()).isEmpty();
    }

    @Test
    void scheduledFundDataReloadDoesNotCacheSkeletonWhenNoQuotesRefresh() {
        var redisTemplate = mockRedis(null);
        var fundDataService = mock(FundDataService.class);
        when(fundDataService.getAllFunds()).thenReturn(List.of());
        when(fundDataService.reloadIfChanged()).thenReturn(true);

        var service = new SnapshotService(fundDataService, mock(NavService.class), mock(QuoteService.class),
                mock(RetryScheduler.class), redisTemplate, new ObjectMapper());

        service.scheduledFundDataReload();

        verify(redisTemplate.opsForValue(), never()).set(eq("fund:snapshot"), anyString());
    }

    @Test
    void holdingContributionUsesPercentageUnitsLikeOtherPctFields() throws Exception {
        var redisTemplate = mockRedis(null);
        var fundDataService = mock(FundDataService.class);
        var quoteService = mock(QuoteService.class);
        var fund = fund("000001", holding("AAPL", "0.10"));
        when(fundDataService.getAllFunds()).thenReturn(List.of(fund));
        when(quoteService.fetchQuotes(List.of("usAAPL")))
                .thenReturn(Map.of("usAAPL", new Quote("usAAPL", "Apple", new BigDecimal("110"), new BigDecimal("100"))));

        var service = new SnapshotService(fundDataService, mock(NavService.class), quoteService,
                mock(RetryScheduler.class), redisTemplate, new ObjectMapper());

        service.reloadFundDataAndRebuildSnapshot();
        var snapshot = service.getSnapshot();

        assertThat(snapshot.getDetails().get("000001").getHoldings().get(0).getChangePct()).isEqualTo(10.0);
        assertThat(snapshot.getDetails().get("000001").getHoldings().get(0).getWeightContribution()).isEqualTo(1.0);
    }

    @Test
    void quoteRefreshComputesFundChangeEvenBeforeNavRefresh() throws Exception {
        var redisTemplate = mockRedis(null);
        var fundDataService = mock(FundDataService.class);
        var quoteService = mock(QuoteService.class);
        var fund = fund("000001", holding("AAPL", "0.10"));
        when(fundDataService.getAllFunds()).thenReturn(List.of(fund));
        when(quoteService.fetchQuotes(List.of("usAAPL")))
                .thenReturn(Map.of("usAAPL", new Quote("usAAPL", "Apple", new BigDecimal("110"), new BigDecimal("100"))));

        var service = new SnapshotService(fundDataService, mock(NavService.class), quoteService,
                mock(RetryScheduler.class), redisTemplate, new ObjectMapper());

        service.reloadFundDataAndRebuildSnapshot();
        var snapshot = service.getSnapshot();

        assertThat(snapshot.getFunds().get(0).getEstimatedChangePct()).isEqualTo(1.0);
        assertThat(snapshot.getDetails().get("000001").getEstimatedChangePct()).isEqualTo(1.0);
    }

    private StringRedisTemplate mockRedis(String cachedPayload) {
        var redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(cachedPayload);
        return redisTemplate;
    }

    private FundData fund(String code, HoldingData holding) {
        var fund = new FundData();
        fund.setFundCode(code);
        fund.setFundName("Test Fund");
        fund.setHoldings(List.of(holding));
        return fund;
    }

    private HoldingData holding(String code, String ratio) {
        var holding = new HoldingData();
        holding.setStockCode(code);
        holding.setStockName(code);
        holding.setRatio(new BigDecimal(ratio));
        return holding;
    }
}
