package org.dsb.fundvaluation;

import org.dsb.fundvaluation.model.FundData;
import org.dsb.fundvaluation.model.HoldingData;
import org.dsb.fundvaluation.model.Quote;
import org.dsb.fundvaluation.service.FundDataService;
import org.dsb.fundvaluation.service.OverseasValuationService;
import org.dsb.fundvaluation.service.QuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OverseasValuationServiceTest {

    @Test
    void cachedCloseDataCanFeedClosedMarketValuation() {
        var quoteService = mock(QuoteService.class);
        var fundDataService = mock(FundDataService.class);
        var redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);

        var fund = overseasFund("501225", "海外科技", List.of(
                holding("NVDA", "英伟达", "0.1000"),
                holding("MSFT", "微软", "0.0500")
        ));
        String cachePayload = "usNVDA,NVIDIA,110,100|usMSFT,Microsoft,90,100";

        when(fundDataService.getAllFunds()).thenReturn(List.of(fund));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("fund:us:close:" + LocalDate.now())).thenReturn(cachePayload);

        var service = new OverseasValuationService(quoteService, fundDataService, redisTemplate);

        List<OverseasValuationService.OverseasFundValuation> valuations =
                service.getOverseasFundValuations();

        assertEquals(1, valuations.size());
        var valuation = valuations.get(0);
        assertEquals(0.50, valuation.getEstimatedChangePct(), 0.001);
        assertEquals(0.50, valuation.getUsContribution(), 0.001);
        assertEquals("ok", valuation.getHoldings().get(0).getStatus());
        assertEquals("ok", valuation.getHoldings().get(1).getStatus());
    }

    @Test
    void missingCachedCloseDataFallsBackToQuoteService() throws Exception {
        var quoteService = mock(QuoteService.class);
        var fundDataService = mock(FundDataService.class);
        var redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);

        var fund = overseasFund("501225", "海外科技", List.of(
                holding("NVDA", "英伟达", "0.1000")
        ));

        when(fundDataService.getAllFunds()).thenReturn(List.of(fund));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("fund:us:close:" + LocalDate.now())).thenReturn(null);
        when(quoteService.fetchQuotes(List.of("usNVDA"))).thenReturn(Map.of(
                "usNVDA", new Quote("usNVDA", "NVIDIA", new BigDecimal("110"), new BigDecimal("100"))
        ));

        var service = new OverseasValuationService(quoteService, fundDataService, redisTemplate);

        var valuation = service.getOverseasFundValuations().get(0);

        assertEquals(1.00, valuation.getEstimatedChangePct(), 0.001);
        assertEquals("ok", valuation.getHoldings().get(0).getStatus());
        verify(quoteService).fetchQuotes(List.of("usNVDA"));
    }

    @Test
    void partialCachedCloseDataFetchesMissingSymbols() throws Exception {
        var quoteService = mock(QuoteService.class);
        var fundDataService = mock(FundDataService.class);
        var redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);

        var fund = overseasFund("501225", "海外科技", List.of(
                holding("NVDA", "英伟达", "0.1000"),
                holding("MSFT", "微软", "0.0500")
        ));

        when(fundDataService.getAllFunds()).thenReturn(List.of(fund));
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("fund:us:close:" + LocalDate.now()))
                .thenReturn("usNVDA,NVIDIA,110,100");
        when(quoteService.fetchQuotes(List.of("usMSFT"))).thenReturn(Map.of(
                "usMSFT", new Quote("usMSFT", "Microsoft", new BigDecimal("90"), new BigDecimal("100"))
        ));

        var service = new OverseasValuationService(quoteService, fundDataService, redisTemplate);

        var valuation = service.getOverseasFundValuations().get(0);

        assertEquals(0.50, valuation.getEstimatedChangePct(), 0.001);
        assertEquals("ok", valuation.getHoldings().get(0).getStatus());
        assertEquals("ok", valuation.getHoldings().get(1).getStatus());
        verify(quoteService).fetchQuotes(List.of("usMSFT"));
    }

    @Test
    void earlyMorningBeijingTimeDuringUsSessionIsMarketOpen() throws Exception {
        var service = new OverseasValuationService(
                mock(QuoteService.class),
                mock(FundDataService.class),
                mock(StringRedisTemplate.class)
        );
        var method = OverseasValuationService.class.getDeclaredMethod("determineTimePeriod", ZonedDateTime.class);
        method.setAccessible(true);
        var beijingTime = ZonedDateTime.of(2026, 6, 2, 1, 30, 0, 0, ZoneId.of("Asia/Shanghai"));

        Object period = method.invoke(service, beijingTime);

        assertEquals(OverseasValuationService.TimePeriod.US_MARKET_OPEN, period);
    }

    @Test
    void cacheUsCloseDataWritesReadablePayloadForTodayAndTomorrow() {
        var fundDataService = mock(FundDataService.class);
        var redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        var service = new OverseasValuationService(
                mock(QuoteService.class),
                fundDataService,
                redisTemplate
        );

        service.cacheUsCloseData(Map.of(
                "usNVDA", new Quote("usNVDA", "NVIDIA", new BigDecimal("110"), new BigDecimal("100"))
        ));

        org.mockito.Mockito.verify(valueOps).set(
                org.mockito.Mockito.eq("fund:us:close:" + LocalDate.now()),
                org.mockito.Mockito.eq("usNVDA,NVIDIA,110,100")
        );
        org.mockito.Mockito.verify(valueOps).set(
                org.mockito.Mockito.eq("fund:us:close:" + LocalDate.now().plusDays(1)),
                org.mockito.Mockito.eq("usNVDA,NVIDIA,110,100")
        );
    }

    private FundData overseasFund(String code, String name, List<HoldingData> holdings) {
        FundData fund = new FundData();
        fund.setFundCode(code);
        fund.setFundName(name);
        fund.setTags(List.of("overseas", "qdii"));
        fund.setHoldings(holdings);
        return fund;
    }

    private HoldingData holding(String code, String name, String ratio) {
        HoldingData holding = new HoldingData();
        holding.setStockCode(code);
        holding.setStockName(name);
        holding.setRatio(new BigDecimal(ratio));
        return holding;
    }
}
