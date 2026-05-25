package org.dsb.fundvaluation.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@Service
public class FundFileService {

    private static final Logger log = LoggerFactory.getLogger(FundFileService.class);

    private final ObjectMapper objectMapper;
    private final Path dataDir;

    public FundFileService(ObjectMapper objectMapper,
                           @Value("${fund.data.dir:./data/funds}") String dataDirPath) {
        this.objectMapper = objectMapper;
        this.dataDir = Path.of(dataDirPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(dataDir);
            log.info("Fund data directory: {}", dataDir);
        } catch (IOException e) {
            log.error("Failed to create fund data directory: {}", dataDir, e);
        }
    }

    public List<Map<String, String>> listFunds() throws IOException {
        List<Map<String, String>> result = new ArrayList<>();
        List<Path> jsonFiles;
        try (Stream<Path> paths = Files.list(dataDir)) {
            jsonFiles = paths.filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        for (Path p : jsonFiles) {
            try {
                var node = objectMapper.readTree(p.toFile());
                String code = node.has("fund_code") ? node.get("fund_code").asText() : "";
                String name = node.has("fund_name") ? node.get("fund_name").asText() : "";
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("fund_code", code);
                entry.put("fund_name", name);
                entry.put("file_name", p.getFileName().toString());
                result.add(entry);
            } catch (Exception e) {
                log.warn("Failed to read fund file: {}", p, e);
            }
        }
        return result;
    }

    public Optional<ObjectNode> getFund(String fundCode) throws IOException {
        Path file = dataDir.resolve(fundCode + ".json");
        if (Files.exists(file)) {
            return Optional.of((ObjectNode) objectMapper.readTree(file.toFile()));
        }
        return Optional.empty();
    }

    public void saveFund(ObjectNode fundNode) throws IOException {
        String code = fundNode.get("fund_code").asText();
        Path file = dataDir.resolve(code + ".json");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(fundNode);
        Files.writeString(file, json, StandardCharsets.UTF_8);
        log.info("Saved fund: {}", code);
    }

    public void deleteFund(String fundCode) throws IOException {
        Path file = dataDir.resolve(fundCode + ".json");
        if (Files.exists(file)) {
            Files.delete(file);
            log.info("Deleted fund: {}", fundCode);
        } else {
            throw new NoSuchFileException("Fund not found: " + fundCode);
        }
    }

    public boolean exists(String fundCode) {
        return Files.exists(dataDir.resolve(fundCode + ".json"));
    }
}