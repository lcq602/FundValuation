package org.dsb.fundvaluation.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Quote {
    private String symbol;
    private String name;
    private BigDecimal lastPrice;
    private BigDecimal prevClose;

    public Quote(String symbol, String name, BigDecimal lastPrice, BigDecimal prevClose) {
        this.symbol = symbol;
        this.name = name;
        this.lastPrice = lastPrice;
        this.prevClose = prevClose;
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public BigDecimal getLastPrice() { return lastPrice; }
    public BigDecimal getPrevClose() { return prevClose; }

    public BigDecimal getChangePct() {
        if (prevClose.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return lastPrice.subtract(prevClose)
                .divide(prevClose, 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
}