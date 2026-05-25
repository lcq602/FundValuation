package org.dsb.fundvaluation;

import org.dsb.fundvaluation.model.EstimateResult;
import org.dsb.fundvaluation.model.HoldingData;
import org.dsb.fundvaluation.model.Quote;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstimateResultTest {

    @Test
    void calculateWithSingleHolding() {
        var holdings = List.of(holding("AAPL", new BigDecimal("1.0")));
        var quotes = Map.of(
                "usAAPL", new Quote("usAAPL", "Apple", new BigDecimal("200"), new BigDecimal("100"))
        );

        var result = EstimateResult.calculate(new BigDecimal("1.0"), holdings, quotes);

        // stock return = (200-100)/100 = 1.0 (100%), weighted return = 1.0 * 1.0 = 1.0
        // estimated_change_pct = 100%, estimated_nav = 1.0 * (1+1.0) = 2.0
        assertEquals(0, new BigDecimal("100.00").compareTo(result.getEstimatedChangePct()));
        assertEquals(0, new BigDecimal("2.0000").compareTo(result.getEstimatedNav()));
    }

    @Test
    void calculateWithMultipleHoldings() {
        var holdings = List.of(
                holding("AAPL", new BigDecimal("0.6")),
                holding("MSFT", new BigDecimal("0.4"))
        );
        var quotes = Map.of(
                "usAAPL", new Quote("usAAPL", "Apple", new BigDecimal("120"), new BigDecimal("100")),
                "usMSFT", new Quote("usMSFT", "Microsoft", new BigDecimal("90"), new BigDecimal("100"))
        );

        var result = EstimateResult.calculate(new BigDecimal("2.0"), holdings, quotes);

        // Apple: (120-100)/100 = 0.2, contribution = 0.6 * 0.2 = 0.12
        // MSFT: (90-100)/100 = -0.1, contribution = 0.4 * -0.1 = -0.04
        // total_weighted_return = 0.08
        // change_pct = 8% => 8.00
        // nav = 2.0 * 1.08 = 2.1600
        assertEquals(0, new BigDecimal("8.00").compareTo(result.getEstimatedChangePct()));
        assertEquals(0, new BigDecimal("2.1600").compareTo(result.getEstimatedNav()));
    }

    @Test
    void calculateWithZeroPrevClose() {
        var holdings = List.of(holding("AAPL", new BigDecimal("1.0")));
        var quotes = Map.of(
                "usAAPL", new Quote("usAAPL", "Apple", new BigDecimal("100"), BigDecimal.ZERO)
        );

        var result = EstimateResult.calculate(new BigDecimal("1.0"), holdings, quotes);

        // prevClose=0 => skip this stock => change_pct=0, nav=1.0
        assertEquals(0, new BigDecimal("0.00").compareTo(result.getEstimatedChangePct()));
        assertEquals(0, new BigDecimal("1.0000").compareTo(result.getEstimatedNav()));
    }

    @Test
    void calculateWithPartialHoldingRatios() {
        // Simulates real fund where holdings don't sum to 100%
        // e.g. fund 000001 has ratios summing to 0.55
        var holdings = List.of(
                holding("TSM", new BigDecimal("0.102")),
                holding("AAPL", new BigDecimal("0.095")),
                holding("MSFT", new BigDecimal("0.089"))
        );
        var quotes = Map.of(
                "usTSM", new Quote("usTSM", "TSMC", new BigDecimal("110"), new BigDecimal("100")),
                "usAAPL", new Quote("usAAPL", "Apple", new BigDecimal("105"), new BigDecimal("100")),
                "usMSFT", new Quote("usMSFT", "Microsoft", new BigDecimal("95"), new BigDecimal("100"))
        );

        var result = EstimateResult.calculate(new BigDecimal("1.0"), holdings, quotes);

        // TSM: (110-100)/100 = 0.10, contribution = 0.102 * 0.10 = 0.0102
        // AAPL: (105-100)/100 = 0.05, contribution = 0.095 * 0.05 = 0.00475
        // MSFT: (95-100)/100 = -0.05, contribution = 0.089 * -0.05 = -0.00445
        // total = 0.0102 + 0.00475 - 0.00445 = 0.0105
        // change_pct = 1.05%, nav = 1.0 * 1.0105 = 1.0105
        assertEquals(0, new BigDecimal("1.05").compareTo(result.getEstimatedChangePct()));
        assertEquals(0, new BigDecimal("1.0105").compareTo(result.getEstimatedNav()));
    }

    @Test
    void calculateWithMissingQuote() {
        var holdings = List.of(holding("AAPL", new BigDecimal("1.0")));
        var quotes = Map.<String, Quote>of(); // no quotes

        var result = EstimateResult.calculate(new BigDecimal("1.0"), holdings, quotes);

        // No quotes => skip => change_pct=0, nav=1.0
        assertEquals(0, new BigDecimal("0.00").compareTo(result.getEstimatedChangePct()));
        assertEquals(0, new BigDecimal("1.0000").compareTo(result.getEstimatedNav()));
    }

    @Test
    void buildTencentSymbolForAShare() {
        assertEquals("sh600519", EstimateResult.buildTencentSymbol("600519"));
        assertEquals("sh900001", EstimateResult.buildTencentSymbol("900001"));
        assertEquals("sz000001", EstimateResult.buildTencentSymbol("000001"));
        assertEquals("sz002001", EstimateResult.buildTencentSymbol("002001"));
        assertEquals("sz300001", EstimateResult.buildTencentSymbol("300001"));
    }

    @Test
    void buildTencentSymbolForHK() {
        assertEquals("hk00700", EstimateResult.buildTencentSymbol("00700"));
        assertEquals("hk09988", EstimateResult.buildTencentSymbol("09988"));
    }

    @Test
    void buildTencentSymbolForUS() {
        assertEquals("usAAPL", EstimateResult.buildTencentSymbol("AAPL"));
        assertEquals("usTSM", EstimateResult.buildTencentSymbol("TSM"));
        assertEquals("usMSFT", EstimateResult.buildTencentSymbol("msft"));
    }

    private HoldingData holding(String code, BigDecimal ratio) {
        HoldingData h = new HoldingData();
        h.setStockCode(code);
        h.setStockName(code);
        h.setRatio(ratio);
        return h;
    }
}