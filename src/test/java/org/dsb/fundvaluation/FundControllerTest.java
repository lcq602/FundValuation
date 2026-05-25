package org.dsb.fundvaluation;

import org.dsb.fundvaluation.dto.*;
import org.dsb.fundvaluation.controller.FundController;
import org.dsb.fundvaluation.service.NewsService;
import org.dsb.fundvaluation.service.SnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FundController.class)
class FundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SnapshotService snapshotService;

    @MockitoBean
    private NewsService newsService;

    @Test
    void fundsEndpointReturnsExpectedStructure() throws Exception {
        var summary = new FundSummary();
        summary.setFundCode("000001");
        summary.setFundName("Test Fund");
        summary.setEstimatedNav(1.2345);
        summary.setEstimatedChangePct(0.56);
        summary.setUpdatedAt("2026-01-01");

        var snapshot = new SnapshotResponse();
        snapshot.setGeneratedAt(1234567890.0);
        snapshot.setFunds(List.of(summary));
        snapshot.setDetails(Map.of());

        when(snapshotService.getSnapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/funds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated_at").value(1234567890.0))
                .andExpect(jsonPath("$.funds[0].fund_code").value("000001"))
                .andExpect(jsonPath("$.funds[0].fund_name").value("Test Fund"))
                .andExpect(jsonPath("$.funds[0].estimated_nav").value(1.2345))
                .andExpect(jsonPath("$.funds[0].estimated_change_pct").value(0.56))
                .andExpect(jsonPath("$.funds[0].updated_at").value("2026-01-01"));
    }

    @Test
    void snapshotEndpointReturnsFullStructure() throws Exception {
        var detail = new FundDetail();
        detail.setFundCode("000001");
        detail.setFundName("Test Fund");
        detail.setBaseNav(1.0);
        detail.setEstimatedNav(1.05);
        detail.setEstimatedChangePct(5.0);
        detail.setUpdatedAt("2026-01-01");
        detail.setStatus("ok");
        detail.setError("");

        var hd = new HoldingDetail();
        hd.setStockCode("AAPL");
        hd.setStockName("Apple");
        hd.setRatio(0.5);
        hd.setMarketSymbol("usAAPL");
        hd.setLastPrice(150);
        hd.setPrevClose(140);
        hd.setChangePct(7.14);
        hd.setWeightContribution(0.0357);
        hd.setStatus("ok");
        detail.setHoldings(List.of(hd));

        var summary = new FundSummary();
        summary.setFundCode("000001");
        summary.setFundName("Test Fund");
        summary.setEstimatedNav(1.05);
        summary.setEstimatedChangePct(5.0);
        summary.setUpdatedAt("2026-01-01");

        var snapshot = new SnapshotResponse();
        snapshot.setGeneratedAt(1234567890.0);
        snapshot.setFunds(List.of(summary));
        snapshot.setDetails(Map.of("000001", detail));

        when(snapshotService.getSnapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated_at").value(1234567890.0))
                .andExpect(jsonPath("$.funds[0].fund_code").value("000001"))
                .andExpect(jsonPath("$.details['000001'].fund_code").value("000001"))
                .andExpect(jsonPath("$.details['000001'].holdings[0].stock_code").value("AAPL"))
                .andExpect(jsonPath("$.details['000001'].holdings[0].status").value("ok"))
                .andExpect(jsonPath("$.details['000001'].status").value("ok"));
    }

    @Test
    void estimateEndpointWithoutFundCodeReturnsAll() throws Exception {
        var detail = new FundDetail();
        detail.setFundCode("000001");
        detail.setFundName("Test Fund");
        detail.setBaseNav(1.0);
        detail.setEstimatedNav(1.05);
        detail.setEstimatedChangePct(5.0);
        detail.setUpdatedAt("2026-01-01");
        detail.setStatus("ok");
        detail.setError("");
        detail.setHoldings(List.of());

        var snapshot = new SnapshotResponse();
        snapshot.setGeneratedAt(1234567890.0);
        snapshot.setFunds(List.of());
        snapshot.setDetails(Map.of("000001", detail));

        when(snapshotService.getSnapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/estimate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected_fund_code").value("000001"))
                .andExpect(jsonPath("$.funds[0].fund_code").value("000001"));
    }

    @Test
    void estimateEndpointWithFundCodeReturnsSpecific() throws Exception {
        var detail = new FundDetail();
        detail.setFundCode("000001");
        detail.setFundName("Test Fund");
        detail.setBaseNav(1.0);
        detail.setEstimatedNav(1.05);
        detail.setEstimatedChangePct(5.0);
        detail.setStatus("ok");
        detail.setError("");
        detail.setHoldings(List.of());

        var snapshot = new SnapshotResponse();
        snapshot.setGeneratedAt(1234567890.0);
        snapshot.setFunds(List.of());
        snapshot.setDetails(Map.of("000001", detail));

        when(snapshotService.getSnapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/estimate").param("fund_code", "000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected_fund_code").value("000001"))
                .andExpect(jsonPath("$.funds[0].fund_code").value("000001"));
    }

    @Test
    void estimateEndpointWithUnknownFundCodeReturnsEmpty() throws Exception {
        var snapshot = new SnapshotResponse();
        snapshot.setGeneratedAt(0);
        snapshot.setFunds(List.of());
        snapshot.setDetails(Map.of());

        when(snapshotService.getSnapshot()).thenReturn(snapshot);

        mockMvc.perform(get("/api/estimate").param("fund_code", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selected_fund_code").value(""))
                .andExpect(jsonPath("$.funds").isEmpty());
    }

    @Test
    void newsEndpointReturnsCachedNews() throws Exception {
        var item = new NewsItem();
        item.setTitle("AI 算力带动光模块需求增长");
        item.setUrl("https://stock.eastmoney.com/a/202605253748075277.html");
        item.setSource("东方财富");
        item.setCategory("股票");
        item.setPublishedAt("");

        var response = new NewsResponse();
        response.setQuery("AI 光模块");
        response.setGeneratedAt(1234567890.0);
        response.setItems(List.of(item));
        response.setError("");

        when(newsService.search("AI 光模块", 5)).thenReturn(response);

        mockMvc.perform(get("/api/news").param("query", "AI 光模块").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("AI 光模块"))
                .andExpect(jsonPath("$.generated_at").value(1234567890.0))
                .andExpect(jsonPath("$.items[0].title").value("AI 算力带动光模块需求增长"))
                .andExpect(jsonPath("$.items[0].source").value("东方财富"))
                .andExpect(jsonPath("$.items[0].category").value("股票"))
                .andExpect(jsonPath("$.error").value(""));
    }
}
