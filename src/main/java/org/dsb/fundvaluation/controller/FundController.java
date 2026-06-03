package org.dsb.fundvaluation.controller;

import org.dsb.fundvaluation.dto.EstimateResponse;
import org.dsb.fundvaluation.dto.FundsResponse;
import org.dsb.fundvaluation.dto.NewsContentResponse;
import org.dsb.fundvaluation.dto.NewsResponse;
import org.dsb.fundvaluation.dto.SnapshotResponse;
import org.dsb.fundvaluation.service.NewsService;
import org.dsb.fundvaluation.service.SnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FundController {

    private final SnapshotService snapshotService;
    private final NewsService newsService;

    public FundController(SnapshotService snapshotService, NewsService newsService) {
        this.snapshotService = snapshotService;
        this.newsService = newsService;
    }

    @GetMapping("/funds")
    public FundsResponse getFunds() {
        var snapshot = snapshotService.getSnapshot();
        var response = new FundsResponse();
        response.setGeneratedAt(snapshot.getGeneratedAt());
        response.setFunds(snapshot.getFunds());
        return response;
    }

    @GetMapping("/snapshot")
    public SnapshotResponse getSnapshot() {
        return snapshotService.getSnapshot();
    }

    @GetMapping("/news")
    public NewsResponse getNews(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "limit", defaultValue = "12") int limit) {
        return newsService.search(query, limit);
    }

    @GetMapping("/news/content")
    public NewsContentResponse getNewsContent(@RequestParam("url") String url) {
        return newsService.getArticleContent(url);
    }

    @GetMapping("/estimate")
    public EstimateResponse getEstimate(@RequestParam(value = "fund_code", required = false) String fundCode) {
        var snapshot = snapshotService.getSnapshot();
        var response = new EstimateResponse();

        if (fundCode != null && !fundCode.isBlank()) {
            var detail = snapshot.getDetails().get(fundCode);
            response.setFunds(detail != null ? List.of(detail) : List.of());
            response.setSelectedFundCode(detail != null ? fundCode : "");
        } else {
            var funds = snapshot.getDetails().values().stream().toList();
            response.setFunds(funds);
            response.setSelectedFundCode(funds.isEmpty() ? "" : funds.get(0).getFundCode());
        }

        return response;
    }
}
