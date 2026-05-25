package org.dsb.fundvaluation.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

public class EstimateResult {
    private final BigDecimal estimatedChangePct;
    private final BigDecimal estimatedNav;

    public EstimateResult(BigDecimal estimatedChangePct, BigDecimal estimatedNav) {
        this.estimatedChangePct = estimatedChangePct;
        this.estimatedNav = estimatedNav;
    }

    public BigDecimal getEstimatedChangePct() { return estimatedChangePct; }
    public BigDecimal getEstimatedNav() { return estimatedNav; }

    /**
     * Calculates estimated fund NAV and change percentage using raw holding ratios.
     *
     * Each holding's contribution = ratio * stockReturn (ratio is already the actual weight in the fund).
     * The total weighted return = sum of all contributions.
     * estimatedNav = baseNav * (1 + totalWeightedReturn).
     */
    public static EstimateResult calculate(BigDecimal baseNav, List<HoldingData> holdings,
                                           Map<String, Quote> quotes) {
        BigDecimal totalWeightedReturn = BigDecimal.ZERO;

        for (HoldingData holding : holdings) {
            Quote quote = quotes.get(buildTencentSymbol(holding.getStockCode()));
            if (quote == null) continue;

            BigDecimal stockReturn = BigDecimal.ZERO;
            if (quote.getPrevClose().compareTo(BigDecimal.ZERO) != 0) {
                stockReturn = quote.getLastPrice().subtract(quote.getPrevClose())
                        .divide(quote.getPrevClose(), 10, RoundingMode.HALF_UP);
            }
            // Use raw ratio as weight — it's the actual portfolio weight
            totalWeightedReturn = totalWeightedReturn.add(holding.getRatio().multiply(stockReturn));
        }

        BigDecimal estimatedChangePct = totalWeightedReturn.multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedNav = baseNav.multiply(BigDecimal.ONE.add(totalWeightedReturn))
                .setScale(4, RoundingMode.HALF_UP);

        return new EstimateResult(estimatedChangePct, estimatedNav);
    }

    public static String buildTencentSymbol(String stockCode) {
        String code = stockCode.trim().toUpperCase();
        if (code.matches("\\d+")) {
            if (code.length() == 5) {
                return "hk" + code;
            }
            return (code.startsWith("6") || code.startsWith("9")) ? "sh" + code : "sz" + code;
        }
        return "us" + code;
    }
}