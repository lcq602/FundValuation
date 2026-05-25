package org.dsb.fundvaluation.dto;

import java.util.List;

public class NewsResponse {
    private String query;
    private double generatedAt;
    private List<NewsItem> items;
    private String error;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public double getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(double generatedAt) { this.generatedAt = generatedAt; }
    public List<NewsItem> getItems() { return items; }
    public void setItems(List<NewsItem> items) { this.items = items; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
