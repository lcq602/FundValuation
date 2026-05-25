package org.dsb.fundvaluation.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.dsb.fundvaluation.model.FundData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FundDataService {

    private static final Logger log = LoggerFactory.getLogger(FundDataService.class);
    private static final String FUNDS_DIR_PATTERN = "classpath:funds/*.json";
    private static final String FALLBACK_FILE = "classpath:fund_holdings.json";

    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourceResolver;
    private List<FundData> funds = Collections.emptyList();

    public FundDataService(ObjectMapper objectMapper, ResourcePatternResolver resourceResolver) {
        this.objectMapper = objectMapper;
        this.resourceResolver = resourceResolver;
    }

    @PostConstruct
    public void loadFunds() {
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
        if (node.has("fund_code") && node.has("fund_name")) {
            return List.of(objectMapper.convertValue(node, FundData.class));
        }
        if (node.isArray()) {
            return objectMapper.convertValue(node, new TypeReference<List<FundData>>() {});
        }
        if (node.has("funds") && node.get("funds").isArray()) {
            return objectMapper.convertValue(node.get("funds"), new TypeReference<List<FundData>>() {});
        }
        return Collections.emptyList();
    }

    public List<FundData> getAllFunds() {
        return funds;
    }
}