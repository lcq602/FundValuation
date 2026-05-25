package org.dsb.fundvaluation.dto;

import java.util.List;
import java.util.Map;

public class SnapshotResponse {
    private double generatedAt;
    private List<FundSummary> funds;
    private Map<String, FundDetail> details;

    public double getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(double generatedAt) { this.generatedAt = generatedAt; }
    public List<FundSummary> getFunds() { return funds; }
    public void setFunds(List<FundSummary> funds) { this.funds = funds; }
    public Map<String, FundDetail> getDetails() { return details; }
    public void setDetails(Map<String, FundDetail> details) { this.details = details; }
}