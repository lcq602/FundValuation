package org.dsb.fundvaluation.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.dsb.fundvaluation.model.FundData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class FundDataService {

    private static final Logger log = LoggerFactory.getLogger(FundDataService.class);
    private static final String FUNDS_DIR_PATTERN = "classpath:funds/*.json";
    private static final String FALLBACK_FILE = "classpath:fund_holdings.json";

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourceResolver;
    private final Path dataDir;
    private List<FundData> funds = Collections.emptyList();
    private String externalDataFingerprint = "";

    public FundDataService(ObjectMapper objectMapper, ResourcePatternResolver resourceResolver,
                           @Value("${fund.data.dir:./data/funds}") String dataDirPath) {
        this.objectMapper = objectMapper;
        this.resourceResolver = resourceResolver;
        this.dataDir = Path.of(dataDirPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void loadFunds() {
        List<FundData> externalFunds = loadExternalFunds();
        if (!externalFunds.isEmpty()) {
            this.funds = externalFunds;
            this.externalDataFingerprint = fingerprintExternalFiles();
            log.info("Loaded {} funds from external directory {}", externalFunds.size(), dataDir);
            return;
        }

        try {
            Resource[] resources = resourceResolver.getResources(FUNDS_DIR_PATTERN);
            if (resources.length == 0) {
                log.warn("No fund files found matching {}, trying fallback", FUNDS_DIR_PATTERN);
                loadFallback();
                return;
            }

            List<FundData> result = new ArrayList<>();
            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    var node = objectMapper.readTree(is);
                    List<FundData> parsed = parseFundPayload(node);
                    for (FundData fund : parsed) {
                        fund.setFileName(resource.getFilename());
                    }
                    result.addAll(parsed);
                }
            }
            this.funds = result;
            log.info("Loaded {} funds from {} files", result.size(), resources.length);
        } catch (Exception e) {
            log.error("Failed to load fund data, trying fallback", e);
            loadFallback();
        }
    }

    public boolean reloadIfChanged() {
        String currentFingerprint = fingerprintExternalFiles();
        if (Objects.equals(currentFingerprint, externalDataFingerprint)) {
            return false;
        }
        loadFunds();
        return true;
    }

    private List<FundData> loadExternalFunds() {
        if (!Files.isDirectory(dataDir)) {
            log.warn("External fund data directory does not exist: {}", dataDir);
            return List.of();
        }

        try (Stream<Path> paths = Files.list(dataDir)) {
            List<Path> jsonFiles = paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();

            List<FundData> result = new ArrayList<>();
            for (Path jsonFile : jsonFiles) {
                try (InputStream is = Files.newInputStream(jsonFile)) {
                    var node = objectMapper.readTree(is);
                    List<FundData> parsed = parseFundPayload(node);
                    for (FundData fund : parsed) {
                        fund.setFileName(jsonFile.getFileName().toString());
                    }
                    result.addAll(parsed);
                }
            }
            return result;
        } catch (IOException e) {
            log.error("Failed to load external fund data from {}", dataDir, e);
            return List.of();
        }
    }

    private String fingerprintExternalFiles() {
        if (!Files.isDirectory(dataDir)) {
            return "";
        }
        try (Stream<Path> paths = Files.list(dataDir)) {
            StringBuilder fingerprint = new StringBuilder();
            for (Path path : paths
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList()) {
                fingerprint.append(path.getFileName())
                        .append(':')
                        .append(Files.size(path))
                        .append(':')
                        .append(Files.getLastModifiedTime(path).toMillis())
                        .append(';');
            }
            return fingerprint.toString();
        } catch (IOException e) {
            log.warn("Failed to fingerprint external fund files from {}", dataDir, e);
            return "";
        }
    }

    private void loadFallback() {
        try {
            Resource resource = resourceResolver.getResource(FALLBACK_FILE);
            if (!resource.exists()) {
                log.error("Fallback {} not found", FALLBACK_FILE);
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                var node = objectMapper.readTree(is);
                this.funds = parseFundPayload(node);
                log.info("Loaded {} funds from fallback", this.funds.size());
            }
        } catch (Exception e) {
            log.error("Failed to load fallback fund data", e);
        }
    }

    private List<FundData> parseFundPayload(JsonNode node) {
        if (isFundNode(node)) {
            return List.of(parseFundNode(node));
        }
        if (node.isArray()) {
            List<FundData> result = new ArrayList<>();
            for (JsonNode item : node) {
                if (isFundNode(item)) {
                    result.add(parseFundNode(item));
                }
            }
            return result;
        }
        if (node.has("funds") && node.get("funds").isArray()) {
            List<FundData> result = new ArrayList<>();
            for (JsonNode item : node.get("funds")) {
                if (isFundNode(item)) {
                    result.add(parseFundNode(item));
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private boolean isFundNode(JsonNode node) {
        return node.has("fund_code") || node.has("fundCode") || node.has("fund_name") || node.has("fundName");
    }

    private FundData parseFundNode(JsonNode node) {
        FundData fund = new FundData();
        fund.setFundCode(text(node, "fund_code", "fundCode"));
        fund.setFundName(text(node, "fund_name", "fundName"));

        // 解析 tags
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = node.get("tags");
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tagNode : tagsNode) {
                if (!tagNode.isNull()) {
                    tags.add(tagNode.asText());
                }
            }
        }
        fund.setTags(tags);

        List<org.dsb.fundvaluation.model.HoldingData> holdings = new ArrayList<>();
        JsonNode holdingsNode = node.get("holdings");
        if (holdingsNode != null && holdingsNode.isArray()) {
            for (JsonNode holdingNode : holdingsNode) {
                var holding = new org.dsb.fundvaluation.model.HoldingData();
                holding.setStockCode(text(holdingNode, "stock_code", "stockCode"));
                holding.setStockName(text(holdingNode, "stock_name", "stockName"));
                String ratio = text(holdingNode, "ratio");
                holding.setRatio(ratio.isBlank() ? BigDecimal.ZERO : new BigDecimal(ratio));
                holdings.add(holding);
            }
        }
        fund.setHoldings(holdings);
        return fund;
    }

    private String text(JsonNode node, String... names) {
        for (String name : names) {
            if (node.has(name) && node.get(name) != null && !node.get(name).isNull()) {
                return node.get(name).asText();
            }
        }
        return "";
    }

    public List<FundData> getAllFunds() {
        return funds;
    }

    public FundData getFund(String fundCode) {
        return funds.stream()
                .filter(f -> f.getFundCode().equals(fundCode))
                .findFirst()
                .orElse(null);
    }
}
