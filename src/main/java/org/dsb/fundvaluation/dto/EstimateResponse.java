package org.dsb.fundvaluation.dto;

import java.util.List;

public class EstimateResponse {
    private String selectedFundCode;
    private List<FundDetail> funds;

    public String getSelectedFundCode() { return selectedFundCode; }
    public void setSelectedFundCode(String selectedFundCode) { this.selectedFundCode = selectedFundCode; }
    public List<FundDetail> getFunds() { return funds; }
    public void setFunds(List<FundDetail> funds) { this.funds = funds; }
}