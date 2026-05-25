package org.dsb.fundvaluation.dto;

import java.util.List;

public class FundDetail {
    private String fundCode;
    private String fundName;
    private double baseNav;
    private double estimatedNav;
    private double estimatedChangePct;
    private String updatedAt;
    private String status;
    private String error;
    private List<HoldingDetail> holdings;

    public String getFundCode() { return fundCode; }
    public void setFundCode(String fundCode) { this.fundCode = fundCode; }
    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }
    public double getBaseNav() { return baseNav; }
    public void setBaseNav(double baseNav) { this.baseNav = baseNav; }
    public double getEstimatedNav() { return estimatedNav; }
    public void setEstimatedNav(double estimatedNav) { this.estimatedNav = estimatedNav; }
    public double getEstimatedChangePct() { return estimatedChangePct; }
    public void setEstimatedChangePct(double estimatedChangePct) { this.estimatedChangePct = estimatedChangePct; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public List<HoldingDetail> getHoldings() { return holdings; }
    public void setHoldings(List<HoldingDetail> holdings) { this.holdings = holdings; }
}