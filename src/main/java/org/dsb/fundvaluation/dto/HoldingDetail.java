package org.dsb.fundvaluation.dto;

public class HoldingDetail {
    private String stockCode;
    private String stockName;
    private double ratio;
    private String marketSymbol;
    private double lastPrice;
    private double prevClose;
    private double changePct;
    private double weightContribution;
    private String status;

    public String getStockCode() { return stockCode; }
    public void setStockCode(String stockCode) { this.stockCode = stockCode; }
    public String getStockName() { return stockName; }
    public void setStockName(String stockName) { this.stockName = stockName; }
    public double getRatio() { return ratio; }
    public void setRatio(double ratio) { this.ratio = ratio; }
    public String getMarketSymbol() { return marketSymbol; }
    public void setMarketSymbol(String marketSymbol) { this.marketSymbol = marketSymbol; }
    public double getLastPrice() { return lastPrice; }
    public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }
    public double getPrevClose() { return prevClose; }
    public void setPrevClose(double prevClose) { this.prevClose = prevClose; }
    public double getChangePct() { return changePct; }
    public void setChangePct(double changePct) { this.changePct = changePct; }
    public double getWeightContribution() { return weightContribution; }
    public void setWeightContribution(double weightContribution) { this.weightContribution = weightContribution; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}