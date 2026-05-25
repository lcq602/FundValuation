package org.dsb.fundvaluation.model;

import java.math.BigDecimal;

public class HoldingData {
    private String stockCode;
    private String stockName;
    private BigDecimal ratio;

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public BigDecimal getRatio() { return ratio; }
    public void setRatio(BigDecimal ratio) { this.ratio = ratio; }
}