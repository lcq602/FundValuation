package org.dsb.fundvaluation.service;

import jakarta.annotation.PostConstruct;
import org.dsb.fundvaluation.dto.FundDetail;
import org.dsb.fundvaluation.dto.FundSummary;
import org.dsb.fundvaluation.dto.HoldingDetail;
import org.dsb.fundvaluation.dto.SnapshotResponse;
import org.dsb.fundvaluation.model.EstimateResult;
import org.dsb.fundvaluation.model.FundData;
import org.dsb.fundvaluation.model.HoldingData;
import org.dsb.fundvaluation.model.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MARKET_OPEN_HOUR = 9;
    private static final int MARKET_CLOSE_HOUR = 17;
    private static final String SNAPSHOT_CACHE_KEY = "fund:snapshot";

    private final FundDataService fundDataService;
    private final NavService navService;
    private final QuoteService quoteService;
    private final RetryScheduler retryScheduler;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private SnapshotResponse currentSnapshot = new SnapshotResponse();

    @Value("${quotes.refresh.fast-ms:15000}")
    private long quotesFastMs;

    @Value("${quotes.refresh.slow-ms:300000}")
    private long quotesSlowMs;

    @Value("${nav.refresh.interval-ms:1800000}")
    private long navIntervalMs;

    @Autowired
    public SnapshotService(FundDataService fundDataService, NavService navService,
                           QuoteService quoteService, RetryScheduler retryScheduler,
                           StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.fundDataService = fundDataService;
        this.navService = navService;
        this.quoteService = quoteService;
        this.retryScheduler = retryScheduler;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        buildSkeletonSnapshot();
    }

    // --- Public API ---

    public SnapshotResponse getSnapshot() {
        SnapshotResponse cachedSnapshot = readSnapshotCache();
        if (cachedSnapshot != null) {
            return cachedSnapshot;
        }

        lock.readLock().lock();
        try {
            return copySnapshot(currentSnapshot);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void reloadFundDataAndRebuildSnapshot() {
        fundDataService.loadFunds();
        buildSkeletonSnapshot();
        refreshQuoteData();
    }

    // --- Scheduled refresh methods ---

    @Scheduled(fixedDelayString = "${quotes.refresh.fast-ms:15000}", initialDelay = 3000)
    public void scheduledQuoteRefresh() {
        if (!isMarketHours()) return;
        refreshQuoteData();
    }

    @Scheduled(fixedDelayString = "${quotes.refresh.slow-ms:300000}", initialDelay = 60000)
    public void scheduledQuoteRefreshSlow() {
        if (isMarketHours()) return;
        refreshQuoteData();
    }

    @Scheduled(fixedDelayString = "${nav.refresh.interval-ms:1800000}", initialDelay = 10000)
    public void scheduledNavRefresh() {
        if (!isMarketHours()) return;
        for (FundData fund : fundDataService.getAllFunds()) {
            refreshSingleFundNav(fund, 0);
        }
    }

    @Scheduled(fixedDelayString = "${fund.data.scan.interval-ms:3600000}", initialDelayString = "${fund.data.scan.initial-delay-ms:60000}")
    public void scheduledFundDataReload() {
        if (fundDataService.reloadIfChanged()) {
            log.info("External fund data changed, rebuilding snapshot");
            buildSkeletonSnapshot();
            refreshQuoteData();
        }
    }

    // --- Quote refresh ---

    private void refreshQuoteData() {
        if (fundDataService.getAllFunds().isEmpty()) return;

        var allSymbols = fundDataService.getAllFunds().stream()
                .flatMap(f -> f.getHoldings().stream())
                .map(h -> EstimateResult.buildTencentSymbol(h.getStockCode()))
                .distinct()
                .toList();

        if (allSymbols.isEmpty()) return;

        Map<String, Quote> quotes;
        try {
            quotes = quoteService.fetchQuotes(allSymbols);
        } catch (Exception e) {
            log.warn("Quote refresh failed, keeping old data: {}", e.getMessage());
            return;
        }

        lock.writeLock().lock();
        try {
            for (FundData fund : fundDataService.getAllFunds()) {
                FundDetail detail = currentSnapshot.getDetails().get(fund.getFundCode());
                if (detail == null) continue;

                boolean hadOldNav = detail.getBaseNav() > 0;
                StringBuilder errors = new StringBuilder();
                boolean partial = false;

                var enriched = buildEnrichedHoldings(fund, quotes);
                for (var h : enriched) {
                    if ("missing_quote".equals(h.getStatus())) partial = true;
                }
                detail.setHoldings(enriched);
                detail.setError(errors.toString());

                Map<String, Quote> fundQuotes = new HashMap<>();
                for (HoldingData h : fund.getHoldings()) {
                    String sym = EstimateResult.buildTencentSymbol(h.getStockCode());
                    Quote q = quotes.get(sym);
                    if (q != null) fundQuotes.put(sym, q);
                }

                BigDecimal baseNav = hadOldNav ? BigDecimal.valueOf(detail.getBaseNav()) : BigDecimal.ONE;
                var estimate = EstimateResult.calculate(baseNav, fund.getHoldings(), fundQuotes);
                detail.setEstimatedChangePct(estimate.getEstimatedChangePct().doubleValue());
                if (hadOldNav) {
                    detail.setEstimatedNav(estimate.getEstimatedNav().doubleValue());
                }

                if (!"ok".equals(detail.getStatus()) && detail.getBaseNav() > 0) {
                    detail.setStatus(partial ? "partial" : "ok");
                }
            }
            rebuildFundSummaries();
            updateGeneratedAt();
            writeSnapshotCache(currentSnapshot);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // --- NAV refresh with retry ---

    private void refreshSingleFundNav(FundData fund, int attempt) {
        try {
            var navResult = navService.fetchLatestNav(fund.getFundCode());
            BigDecimal baseNav = navResult.nav();
            String updatedAt = navResult.date();

            lock.writeLock().lock();
            try {
                FundDetail detail = currentSnapshot.getDetails().get(fund.getFundCode());
                if (detail == null) return;

                detail.setBaseNav(baseNav.doubleValue());
                detail.setUpdatedAt(updatedAt);
                detail.setStatus("ok");
                detail.setError("");

                Map<String, Quote> fundQuotes = collectCurrentQuotes(detail);
                var estimate = EstimateResult.calculate(baseNav, fund.getHoldings(), fundQuotes);
                detail.setEstimatedNav(estimate.getEstimatedNav().doubleValue());
                detail.setEstimatedChangePct(estimate.getEstimatedChangePct().doubleValue());

                rebuildFundSummaries();
                updateGeneratedAt();
                writeSnapshotCache(currentSnapshot);
            } finally {
                lock.writeLock().unlock();
            }
            log.info("NAV refreshed for {}: {}", fund.getFundCode(), baseNav);
        } catch (Exception e) {
            log.warn("NAV refresh failed for {} (attempt {}/3): {}",
                    fund.getFundCode(), attempt + 1, e.getMessage());
            if (attempt < 2) {
                retryScheduler.schedule(() -> refreshSingleFundNav(fund, attempt + 1), 30_000);
            }
        }
    }

    // --- Helpers ---

    private List<HoldingDetail> buildEnrichedHoldings(FundData fund, Map<String, Quote> quotes) {
        List<HoldingDetail> enriched = new ArrayList<>();
        for (HoldingData holding : fund.getHoldings()) {
            String symbol = EstimateResult.buildTencentSymbol(holding.getStockCode());
            BigDecimal ratio = holding.getRatio();
            Quote quote = quotes.get(symbol);

            HoldingDetail row = new HoldingDetail();
            row.setStockCode(holding.getStockCode());
            row.setStockName(holding.getStockName());
            row.setRatio(ratio.doubleValue());
            row.setMarketSymbol(symbol);

            if (quote == null) {
                row.setLastPrice(0);
                row.setPrevClose(0);
                row.setChangePct(0);
                row.setWeightContribution(0);
                row.setStatus("missing_quote");
            } else {
                BigDecimal lastPrice = quote.getLastPrice();
                BigDecimal prevClose = quote.getPrevClose();
                BigDecimal stockReturn = BigDecimal.ZERO;
                if (prevClose.compareTo(BigDecimal.ZERO) != 0) {
                    stockReturn = lastPrice.subtract(prevClose)
                            .divide(prevClose, 10, RoundingMode.HALF_UP);
                }
                // Use raw ratio as weight — it's the actual portfolio weight
                BigDecimal contributionPct = ratio.multiply(stockReturn).multiply(new BigDecimal("100"));

                row.setLastPrice(lastPrice.doubleValue());
                row.setPrevClose(prevClose.doubleValue());
                row.setChangePct(quote.getChangePct().doubleValue());
                row.setWeightContribution(contributionPct.doubleValue());
                row.setStatus("ok");
            }
            enriched.add(row);
        }
        return enriched;
    }

    private Map<String, Quote> collectCurrentQuotes(FundDetail detail) {
        Map<String, Quote> result = new HashMap<>();
        if (detail.getHoldings() == null) return result;
        for (HoldingDetail hd : detail.getHoldings()) {
            if ("ok".equals(hd.getStatus()) && hd.getLastPrice() > 0) {
                result.put(hd.getMarketSymbol(), new Quote(
                        hd.getMarketSymbol(), hd.getStockName(),
                        BigDecimal.valueOf(hd.getLastPrice()),
                        BigDecimal.valueOf(hd.getPrevClose())));
            }
        }
        return result;
    }

    private void rebuildFundSummaries() {
        List<FundSummary> summaries = new ArrayList<>();
        for (FundDetail detail : currentSnapshot.getDetails().values()) {
            FundSummary summary = new FundSummary();
            summary.setFundCode(detail.getFundCode());
            summary.setFundName(detail.getFundName());
            summary.setEstimatedNav(detail.getEstimatedNav());
            summary.setEstimatedChangePct(detail.getEstimatedChangePct());
            summary.setUpdatedAt(detail.getUpdatedAt());
            summaries.add(summary);
        }
        currentSnapshot.setFunds(summaries);
    }

    private void updateGeneratedAt() {
        currentSnapshot.setGeneratedAt((double) Instant.now().getEpochSecond());
    }

    private void buildSkeletonSnapshot() {
        Map<String, FundDetail> details = new LinkedHashMap<>();
        List<FundSummary> funds = new ArrayList<>();

        for (FundData fund : fundDataService.getAllFunds()) {
            FundDetail detail = new FundDetail();
            detail.setFundCode(fund.getFundCode());
            detail.setFundName(fund.getFundName());
            detail.setBaseNav(0);
            detail.setEstimatedNav(0);
            detail.setEstimatedChangePct(0);
            detail.setUpdatedAt("");
            detail.setStatus("pending");
            detail.setError("");
            detail.setHoldings(fund.getHoldings().stream().map(h -> {
                HoldingDetail hd = new HoldingDetail();
                hd.setStockCode(h.getStockCode());
                hd.setStockName(h.getStockName());
                hd.setRatio(h.getRatio().doubleValue());
                hd.setMarketSymbol(EstimateResult.buildTencentSymbol(h.getStockCode()));
                hd.setLastPrice(0);
                hd.setPrevClose(0);
                hd.setChangePct(0);
                hd.setWeightContribution(0);
                hd.setStatus("pending");
                return hd;
            }).toList());

            details.put(fund.getFundCode(), detail);

            FundSummary summary = new FundSummary();
            summary.setFundCode(fund.getFundCode());
            summary.setFundName(fund.getFundName());
            summary.setEstimatedNav(0);
            summary.setEstimatedChangePct(0);
            summary.setUpdatedAt("");
            funds.add(summary);
        }

        SnapshotResponse snapshot = new SnapshotResponse();
        snapshot.setGeneratedAt(0);
        snapshot.setFunds(funds);
        snapshot.setDetails(details);

        lock.writeLock().lock();
        try {
            this.currentSnapshot = snapshot;
            writeSnapshotCache(this.currentSnapshot);
        } finally {
            lock.writeLock().unlock();
        }

        log.info("Skeleton snapshot initialized with {} funds", funds.size());
    }

    private SnapshotResponse readSnapshotCache() {
        try {
            String payload = redisTemplate.opsForValue().get(SNAPSHOT_CACHE_KEY);
            if (payload == null || payload.isBlank()) {
                return null;
            }
            SnapshotResponse snapshot = objectMapper.readValue(payload, SnapshotResponse.class);
            return isCacheableSnapshot(snapshot) ? snapshot : null;
        } catch (Exception e) {
            log.warn("Failed to read snapshot cache: {}", e.getMessage());
            return null;
        }
    }

    private void writeSnapshotCache(SnapshotResponse snapshot) {
        if (!isCacheableSnapshot(snapshot)) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(SNAPSHOT_CACHE_KEY, objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            log.warn("Failed to write snapshot cache: {}", e.getMessage());
        }
    }

    private boolean isCacheableSnapshot(SnapshotResponse snapshot) {
        return snapshot != null && snapshot.getGeneratedAt() > 0;
    }

    private SnapshotResponse copySnapshot(SnapshotResponse source) {
        SnapshotResponse copy = new SnapshotResponse();
        copy.setGeneratedAt(source.getGeneratedAt());
        copy.setFunds(source.getFunds() != null ? List.copyOf(source.getFunds()) : List.of());
        copy.setDetails(source.getDetails() != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(source.getDetails()))
                : Map.of());
        return copy;
    }

    static boolean isMarketHours() {
        ZonedDateTime now = ZonedDateTime.now(CHINA_ZONE);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;
        int hour = now.getHour();
        return hour >= MARKET_OPEN_HOUR && hour < MARKET_CLOSE_HOUR;
    }
}
