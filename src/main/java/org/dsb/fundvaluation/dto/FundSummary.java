package org.dsb.fundvaluation.dto;

public class FundSummary {
    private String fundCode;
    private String fundName;
    private double estimatedNav;
    private double estimatedChangePct;
    private String updatedAt;

    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    public double getEstimatedNav() { return estimatedNav; }
    public void setEstimatedNav(double estimatedNav) { this.estimatedNav = estimatedNav; }
    public double getEstimatedChangePct() { return estimatedChangePct; }
    public void setEstimatedChangePct(double estimatedChangePct) { this.estimatedChangePct = estimatedChangePct; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}