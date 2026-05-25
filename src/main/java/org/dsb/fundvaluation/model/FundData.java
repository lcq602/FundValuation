package org.dsb.fundvaluation.model;

import java.util.List;

public class FundData {
    private String fundCode;
    private String fundName;
    private String fileName;
    private List<HoldingData> holdings;

    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public List<HoldingData> getHoldings() { return holdings; }
    public void setHoldings(List<HoldingData> holdings) { this.holdings = holdings; }
}