package org.dsb.fundvaluation.controller;

import org.dsb.fundvaluation.model.Quote;
import org.dsb.fundvaluation.service.OverseasValuationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class OverseasController {

    private final OverseasValuationService overseasService;

    public OverseasController(OverseasValuationService overseasService) {
        this.overseasService = overseasService;
    }

    /**
     * 获取所有海外基金（QDII）的估值数据
     */
    @GetMapping("/overseas")
    public List<OverseasValuationService.OverseasFundValuation> getOverseasFunds() {
        return overseasService.getOverseasFundValuations();
    }

    /**
     * 获取单个海外基金的估值数据
     */
    @GetMapping("/overseas/{fundCode}")
    public OverseasValuationService.OverseasFundValuation getOverseasFund(
            @PathVariable String fundCode) {
        return overseasService.getOverseasFundValuation(fundCode);
    }

    /**
     * 获取美股夜盘实时数据
     */
    @GetMapping("/us-night-market")
    public Map<String, Quote> getUsNightMarket() {
        return overseasService.getUsNightMarketData();
    }

    /**
     * 获取当前时间段信息（用于前端显示状态）
     */
    @GetMapping("/time-period")
    public OverseasValuationService.TimePeriodInfo getTimePeriod() {
        return overseasService.getTimePeriodInfo();
    }
}
