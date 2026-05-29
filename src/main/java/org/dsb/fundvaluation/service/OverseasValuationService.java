package org.dsb.fundvaluation.service;

import org.dsb.fundvaluation.model.FundData;
import org.dsb.fundvaluation.model.HoldingData;
import org.dsb.fundvaluation.model.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OverseasValuationService {

    private static final Logger log = LoggerFactory.getLogger(OverseasValuationService.class);
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    // 美股开盘时间（北京时间，夏令时21:30，冬令时22:30）
    private static final int US_MARKET_OPEN_HOUR_SUMMER = 21;
    private static final int US_MARKET_OPEN_MINUTE_SUMMER = 30;
    private static final int US_MARKET_OPEN_HOUR_WINTER = 22;
    private static final int US_MARKET_OPEN_MINUTE_WINTER = 30;

    // 美股收盘时间（北京时间，夏令时04:00，冬令时05:00）
    private static final int US_MARKET_CLOSE_HOUR_SUMMER = 4;
    private static final int US_MARKET_CLOSE_HOUR_WINTER = 5;

    // A股交易时间
    private static final int A_SHARE_OPEN_HOUR = 9;
    private static final int A_SHARE_CLOSE_HOUR = 15;

    private static final String US_CLOSE_DATA_KEY = "fund:us:close:";
    private static final String A_SHARE_CLOSE_DATA_KEY = "fund:a_share:close:";

    private final QuoteService quoteService;
    private final FundDataService fundDataService;
    private final StringRedisTemplate redisTemplate;

    // 缓存昨夜美股收盘数据
    private final Map<String, Quote> usCloseDataCache = new ConcurrentHashMap<>();
    private LocalDate lastUsCloseDate = LocalDate.now();

    @Autowired
    public OverseasValuationService(QuoteService quoteService,
                                    FundDataService fundDataService,
                                    StringRedisTemplate redisTemplate) {
        this.quoteService = quoteService;
        this.fundDataService = fundDataService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取所有海外基金（QDII基金）的估值数据
     */
    public List<OverseasFundValuation> getOverseasFundValuations() {
        List<OverseasFundValuation> results = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now(CHINA_ZONE);
        TimePeriod period = determineTimePeriod(now);

        for (FundData fund : fundDataService.getAllFunds()) {
            if (!fund.isOverseas()) continue;

            OverseasFundValuation valuation = calculateFundValuation(fund, period);
            results.add(valuation);
        }

        return results;
    }

    /**
     * 获取单个海外基金的估值数据
     */
    public OverseasFundValuation getOverseasFundValuation(String fundCode) {
        FundData fund = fundDataService.getFund(fundCode);
        if (fund == null || !fund.isOverseas()) {
            return null;
        }

        ZonedDateTime now = ZonedDateTime.now(CHINA_ZONE);
        TimePeriod period = determineTimePeriod(now);
        return calculateFundValuation(fund, period);
    }

    /**
     * 获取美股夜盘实时数据
     */
    public Map<String, Quote> getUsNightMarketData() {
        List<String> symbols = new ArrayList<>();
        for (FundData fund : fundDataService.getAllFunds()) {
            if (!fund.isOverseas()) continue;
            for (HoldingData holding : fund.getHoldings()) {
                String symbol = buildUsSymbol(holding.getStockCode());
                if (!symbols.contains(symbol)) {
                    symbols.add(symbol);
                }
            }
        }

        try {
            return quoteService.fetchQuotes(symbols);
        } catch (Exception e) {
            log.warn("Failed to fetch US night market data: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 获取当前时间段的描述
     */
    public TimePeriodInfo getTimePeriodInfo() {
        ZonedDateTime now = ZonedDateTime.now(CHINA_ZONE);
        TimePeriod period = determineTimePeriod(now);

        TimePeriodInfo info = new TimePeriodInfo();
        info.setPeriod(period.name());
        info.setPeriodDescription(period.getDescription());
        info.setChinaTime(now.format(DateTimeFormatter.ofPattern("HH:mm")));
        info.setUsTime(now.withZoneSameInstant(ZoneId.of("America/New_York"))
                .format(DateTimeFormatter.ofPattern("HH:mm")));

        // 判断是否为美股交易日
        DayOfWeek day = now.getDayOfWeek();
        info.setUsMarketDay(day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY);

        return info;
    }

    private OverseasFundValuation calculateFundValuation(FundData fund, TimePeriod period) {
        OverseasFundValuation result = new OverseasFundValuation();
        result.setFundCode(fund.getFundCode());
        result.setFundName(fund.getFundName());
        result.setTags(fund.getTags());
        result.setTimePeriod(period.name());
        result.setTimePeriodDescription(period.getDescription());

        // 获取持仓的美股符号
        List<String> usSymbols = new ArrayList<>();
        List<HoldingData> usHoldings = new ArrayList<>();
        for (HoldingData holding : fund.getHoldings()) {
            String symbol = buildUsSymbol(holding.getStockCode());
            usSymbols.add(symbol);
            usHoldings.add(holding);
        }

        // 获取美股数据
        Map<String, Quote> usQuotes;
        if (period == TimePeriod.US_MARKET_OPEN) {
            // 美股盘中，实时获取
            try {
                usQuotes = quoteService.fetchQuotes(usSymbols);
            } catch (Exception e) {
                log.warn("Failed to fetch real-time US quotes: {}", e.getMessage());
                usQuotes = getCachedUsCloseData();
            }
        } else {
            // 美股未开盘或已收盘，使用缓存数据
            usQuotes = getCachedUsCloseData();
        }

        // 计算美股部分贡献
        BigDecimal usContribution = BigDecimal.ZERO;
        List<OverseasHoldingDetail> holdingDetails = new ArrayList<>();

        for (int i = 0; i < usHoldings.size(); i++) {
            HoldingData holding = usHoldings.get(i);
            String symbol = usSymbols.get(i);
            Quote quote = usQuotes.get(symbol);

            OverseasHoldingDetail detail = new OverseasHoldingDetail();
            detail.setStockCode(holding.getStockCode());
            detail.setStockName(holding.getStockName());
            detail.setRatio(holding.getRatio().doubleValue());

            if (quote != null) {
                BigDecimal stockReturn = calculateReturn(quote);
                BigDecimal contribution = holding.getRatio().multiply(stockReturn)
                        .multiply(new BigDecimal("100"));
                detail.setLastPrice(quote.getLastPrice().doubleValue());
                detail.setChangePct(stockReturn.multiply(new BigDecimal("100")).doubleValue());
                detail.setContribution(contribution.doubleValue());
                detail.setStatus("ok");
                usContribution = usContribution.add(contribution);
            } else {
                detail.setStatus("no_data");
            }
            holdingDetails.add(detail);
        }

        // A股部分贡献（如果有A股持仓）
        BigDecimal aShareContribution = BigDecimal.ZERO;
        // A股数据通过主估值服务获取，这里简化处理

        BigDecimal totalContribution = usContribution.add(aShareContribution);

        result.setEstimatedChangePct(totalContribution.setScale(2, RoundingMode.HALF_UP).doubleValue());
        result.setUsContribution(usContribution.setScale(2, RoundingMode.HALF_UP).doubleValue());
        result.setaShareContribution(aShareContribution.setScale(2, RoundingMode.HALF_UP).doubleValue());
        result.setHoldings(holdingDetails);

        return result;
    }

    private Map<String, Quote> getCachedUsCloseData() {
        // 如果缓存过期，重新加载
        if (usCloseDataCache.isEmpty()) {
            loadCachedUsCloseData();
        }
        return usCloseDataCache;
    }

    private void loadCachedUsCloseData() {
        String dateStr = LocalDate.now().minusDays(1).toString();
        try {
            String cached = redisTemplate.opsForValue().get(US_CLOSE_DATA_KEY + dateStr);
            if (cached != null && !cached.isEmpty()) {
                // 解析缓存的美股收盘数据
                // 格式: symbol,lastPrice,prevClose|symbol2,lastPrice,prevClose|...
                String[] entries = cached.split("\\|");
                for (String entry : entries) {
                    String[] parts = entry.split(",");
                    if (parts.length >= 3) {
                        Quote quote = new Quote(
                                parts[0],
                                parts[1],
                                new BigDecimal(parts[2]),
                                new BigDecimal(parts[3]));
                        usCloseDataCache.put(parts[0], quote);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load cached US close data: {}", e.getMessage());
        }
    }

    public void cacheUsCloseData(Map<String, Quote> quotes) {
        String dateStr = LocalDate.now().toString();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Quote> entry : quotes.entrySet()) {
            if (sb.length() > 0) sb.append("|");
            Quote q = entry.getValue();
            sb.append(entry.getKey()).append(",")
                    .append(q.getName()).append(",")
                    .append(q.getLastPrice()).append(",")
                    .append(q.getPrevClose());
        }
        redisTemplate.opsForValue().set(US_CLOSE_DATA_KEY + dateStr, sb.toString());
        // 缓存到次日
        redisTemplate.opsForValue().set(US_CLOSE_DATA_KEY + LocalDate.now().plusDays(1).toString(), sb.toString());
    }

    private TimePeriod determineTimePeriod(ZonedDateTime now) {
        int hour = now.getHour();
        int minute = now.getMinute();
        DayOfWeek day = now.getDayOfWeek();

        // 周末不交易
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return TimePeriod.WEEKEND;
        }

        // 判断是否夏令时
        boolean isSummerTime = isSummerTime(now.toLocalDate());

        // A股交易时段: 09:30-11:30, 13:00-15:00
        boolean isAShareTime = (hour == 9 && minute >= 30) || (hour >= 10 && hour < 11) ||
                (hour == 11 && minute <= 30) || (hour >= 13 && hour < 15);

        // 美股开盘时间
        int usOpenHour = isSummerTime ? US_MARKET_OPEN_HOUR_SUMMER : US_MARKET_OPEN_HOUR_WINTER;
        int usCloseHour = isSummerTime ? US_MARKET_CLOSE_HOUR_SUMMER : US_MARKET_CLOSE_HOUR_WINTER;

        // 美股盘前（北京时间9:00-21:30/22:30）
        if (hour < usOpenHour) {
            return TimePeriod.US_MARKET_CLOSED;
        }

        // 美股盘中（北京时间21:30/22:30-04:00/05:00）
        if (hour >= usOpenHour || hour < usCloseHour) {
            return TimePeriod.US_MARKET_OPEN;
        }

        return TimePeriod.US_MARKET_CLOSED;
    }

    private boolean isSummerTime(LocalDate date) {
        // 美国夏令时：3月第二个周日到11月第一个周日
        int year = date.getYear();
        LocalDate springForward = LocalDate.of(year, 3, 1);
        // 找到3月第二个周日
        while (springForward.getDayOfWeek() != DayOfWeek.SUNDAY) {
            springForward = springForward.plusDays(1);
        }
        springForward = springForward.plusWeeks(1);

        LocalDate fallBack = LocalDate.of(year, 11, 1);
        // 找到11月第一个周日
        while (fallBack.getDayOfWeek() != DayOfWeek.SUNDAY) {
            fallBack = fallBack.plusDays(1);
        }

        return date.isAfter(springForward.minusDays(1)) && date.isBefore(fallBack);
    }

    private String buildUsSymbol(String stockCode) {
        String code = stockCode.trim().toUpperCase();
        // 港股（5位数字）
        if (code.matches("\\d{5}")) {
            return "hk" + code;
        }
        // A股（6位数字）
        if (code.matches("\\d{6}")) {
            return code.startsWith("6") ? "sh" + code : "sz" + code;
        }
        // 美股（字母）
        return "us" + code;
    }

    private BigDecimal calculateReturn(Quote quote) {
        if (quote.getPrevClose().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return quote.getLastPrice().subtract(quote.getPrevClose())
                .divide(quote.getPrevClose(), 10, RoundingMode.HALF_UP);
    }

    // 时间段枚举
    public enum TimePeriod {
        US_MARKET_OPEN("美股盘中", "美股正在交易中，实时计算收益"),
        US_MARKET_CLOSED("美股已收盘", "使用最新收盘价计算收益"),
        A_SHARE_MARKET_OPEN("A股盘中", "使用A股实时数据"),
        WEEKEND("周末", "数据已固定为上周五收盘");

        private final String name;
        private final String description;

        TimePeriod(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
    }

    // 时间段信息
    public static class TimePeriodInfo {
        private String period;
        private String periodDescription;
        private String chinaTime;
        private String usTime;
        private boolean usMarketDay;

        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public String getPeriodDescription() { return periodDescription; }
        public void setPeriodDescription(String periodDescription) { this.periodDescription = periodDescription; }
        public String getChinaTime() { return chinaTime; }
        public void setChinaTime(String chinaTime) { this.chinaTime = chinaTime; }
        public String getUsTime() { return usTime; }
        public void setUsTime(String usTime) { this.usTime = usTime; }
        public boolean isUsMarketDay() { return usMarketDay; }
        public void setUsMarketDay(boolean usMarketDay) { this.usMarketDay = usMarketDay; }
    }

    // 海外基金估值结果
    public static class OverseasFundValuation {
        private String fundCode;
        private String fundName;
        private List<String> tags;
        private String timePeriod;
        private String timePeriodDescription;
        private double estimatedChangePct;
        private double usContribution;
        private double aShareContribution;
        private List<OverseasHoldingDetail> holdings;

        public String getFundCode() { return fundCode; }
        public void setFundCode(String fundCode) { this.fundCode = fundCode; }
        public String getFundName() { return fundName; }
        public void setFundName(String fundName) { this.fundName = fundName; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getTimePeriod() { return timePeriod; }
        public void setTimePeriod(String timePeriod) { this.timePeriod = timePeriod; }
        public String getTimePeriodDescription() { return timePeriodDescription; }
        public void setTimePeriodDescription(String timePeriodDescription) { this.timePeriodDescription = timePeriodDescription; }
        public double getEstimatedChangePct() { return estimatedChangePct; }
        public void setEstimatedChangePct(double estimatedChangePct) { this.estimatedChangePct = estimatedChangePct; }
        public double getUsContribution() { return usContribution; }
        public void setUsContribution(double usContribution) { this.usContribution = usContribution; }
        public double getaShareContribution() { return aShareContribution; }
        public void setaShareContribution(double aShareContribution) { this.aShareContribution = aShareContribution; }
        public List<OverseasHoldingDetail> getHoldings() { return holdings; }
        public void setHoldings(List<OverseasHoldingDetail> holdings) { this.holdings = holdings; }
    }

    // 海外持仓详情
    public static class OverseasHoldingDetail {
        private String stockCode;
        private String stockName;
        private double ratio;
        private double lastPrice;
        private double changePct;
        private double contribution;
        private String status;

        public String getStockCode() { return stockCode; }
        public void setStockCode(String stockCode) { this.stockCode = stockCode; }
        public String getStockName() { return stockName; }
        public void setStockName(String stockName) { this.stockName = stockName; }
        public double getRatio() { return ratio; }
        public void setRatio(double ratio) { this.ratio = ratio; }
        public double getLastPrice() { return lastPrice; }
        public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }
        public double getChangePct() { return changePct; }
        public void setChangePct(double changePct) { this.changePct = changePct; }
        public double getContribution() { return contribution; }
        public void setContribution(double contribution) { this.contribution = contribution; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
