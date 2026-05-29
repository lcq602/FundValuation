package org.dsb.fundvaluation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FundOnlineService {

    private static final Logger log = LoggerFactory.getLogger(FundOnlineService.class);
    private static final String FUND_INFO_URL = "https://fundgz.1234567.com.cn/js/{code}.js";
    private static final String FUND_HOLDINGS_URL =
            "https://fundf10.eastmoney.com/FundArchivesDatas.aspx?type=jjcc&code={code}&topline=20&year=&month=&rt=";

    private static final Pattern JSONP_PATTERN = Pattern.compile("jsonpgz\\((.+)\\)\\s*;?");
    private static final Pattern APIDATA_PATTERN = Pattern.compile("var\\s+apidata\\s*=\\s*(\\{.+\\})\\s*;?");
    private static final Pattern TABLE_ROW_PATTERN =
            Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TD_CELL_PATTERN =
            Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern STOCK_CODE_PATTERN =
            Pattern.compile("/(?:sh|sz|be|us|hk|)\\(\\d{6}\\)|(\\d{6})|<a[^>]+href=\"[^\"]*(\\d{6})[^\"]*\"[^>]*>");
    private static final Pattern CLEAN_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern FUNDCODE_PATTERN = Pattern.compile("\"fundcode\"\\s*:\\s*\"([^\"]+)\"");

    private final RestTemplate restTemplate;

    public FundOnlineService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> queryFundOnline(String code) {
        Map<String, Object> result = new LinkedHashMap<>();

        // Fetch fund basic info
        String fundName = fetchFundName(code);
        result.put("fund_code", code);
        result.put("fund_name", fundName != null ? fundName : "");

        // Fetch holdings
        List<Map<String, Object>> holdings = fetchHoldings(code);
        result.put("holdings", holdings);

        return result;
    }

    private String fetchFundName(String code) {
        try {
            HttpHeaders headers = buildHeaders();
            var entity = new HttpEntity<Void>(headers);
            String response = restTemplate.exchange(FUND_INFO_URL, HttpMethod.GET, entity, String.class, code).getBody();
            if (response == null) return null;

            Matcher m = NAME_PATTERN.matcher(response);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch fund name for {}: {}", code, e.getMessage());
        }
        return null;
    }

    private List<Map<String, Object>> fetchHoldings(String code) {
        List<Map<String, Object>> holdings = new ArrayList<>();

        try {
            HttpHeaders headers = buildHeaders();
            headers.set(HttpHeaders.REFERER, "https://fundf10.eastmoney.com/" + code + ".html");
            var entity = new HttpEntity<Void>(headers);
            String response = restTemplate.exchange(FUND_HOLDINGS_URL, HttpMethod.GET, entity, String.class, code).getBody();
            if (response == null) return holdings;

            // Parse the response - might be JSONP "var apidata={content:...}" or HTML directly
            String htmlContent = extractHtmlContent(response);
            if (htmlContent == null) return holdings;

            holdings.addAll(parseHoldingsTable(htmlContent));
        } catch (Exception e) {
            log.warn("Failed to fetch holdings for {}: {}", code, e.getMessage());
        }

        return holdings;
    }

    private String extractHtmlContent(String response) {
        // Try JSONP format: var apidata={content:"<html>", ...}
        Matcher apidataMatcher = APIDATA_PATTERN.matcher(response);
        if (apidataMatcher.find()) {
            String jsonPart = apidataMatcher.group(1);
            // Extract content field value
            Pattern contentP = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"\\s*(?:,|})", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher contentM = contentP.matcher(jsonPart);
            if (contentM.find()) {
                String raw = contentM.group(1);
                // Unescape JSON string
                return raw.replace("\\\"", "\"").replace("\\n", "\n").replace("\\t", "\t")
                        .replace("\\r", "\r").replace("\\/", "/").replace("\\\\", "\\");
            }
        }

        // Maybe it's direct HTML
        if (response.contains("<table") || response.contains("<tr")) {
            return response;
        }

        return null;
    }

    private List<Map<String, Object>> parseHoldingsTable(String html) {
        List<Map<String, Object>> holdings = new ArrayList<>();

        // Split by <tr> tags
        Matcher rowMatcher = TABLE_ROW_PATTERN.matcher(html);
        // Skip header row
        boolean isFirst = true;

        while (rowMatcher.find()) {
            String rowHtml = rowMatcher.group(1);
            if (isFirst) {
                isFirst = false;
                // Skip rows without numeric first cell
                continue;
            }

            // Extract td cells
            Matcher cellMatcher = TD_CELL_PATTERN.matcher(rowHtml);
            List<String> cells = new ArrayList<>();
            while (cellMatcher.find()) {
                cells.add(cellMatcher.group(1).trim());
            }

            if (cells.size() < 4) continue;

            // Cell 0: index (skip)
            // Cell 1: stock code
            String stockCode = extractStockCode(cells.get(1));
            // Cell 2: stock name
            String stockName = cleanHtml(cells.get(2));
            // Cell 3: ratio
            String ratioStr = cleanHtml(cells.get(3)).replace("%", "").trim();

            if (stockCode.isEmpty()) continue;

            Map<String, Object> holding = new LinkedHashMap<>();
            holding.put("stock_code", stockCode);
            holding.put("stock_name", stockName);
            try {
                double ratio = Double.parseDouble(ratioStr) / 100.0;
                holding.put("ratio", Math.round(ratio * 10000.0) / 10000.0);
            } catch (NumberFormatException e) {
                holding.put("ratio", 0);
            }

            holdings.add(holding);
        }

        return holdings;
    }

    private String extractStockCode(String tdHtml) {
        // Try to find a stock code in the format of 6 digits
        Matcher m = Pattern.compile("(\\d{6})").matcher(tdHtml);
        if (m.find()) {
            return m.group(1);
        }
        // Fallback: get text content
        return cleanHtml(tdHtml);
    }

    private String cleanHtml(String html) {
        if (html == null) return "";
        return CLEAN_TAG_PATTERN.matcher(html).replaceAll("").trim()
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replaceAll("\\s+", " ").trim();
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        headers.set(HttpHeaders.ACCEPT, "*/*");
        headers.set(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8");
        return headers;
    }
}