package org.dsb.fundvaluation.dto;

import java.util.List;

public class FundsResponse {
    private double generatedAt;
    private List<FundSummary> funds;

    public double getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(double generatedAt) { this.generatedAt = generatedAt; }
    public List<FundSummary> getFunds() { return funds; }
    public void setFunds(List<FundSummary> funds) { this.funds = funds; }
}