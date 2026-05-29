package org.dsb.fundvaluation.controller;

import tools.jackson.databind.node.ObjectNode;
import org.dsb.fundvaluation.service.FundFileService;
import org.dsb.fundvaluation.service.SnapshotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/funds")
public class FundAdminController {

    private final FundFileService fundFileService;
    private final SnapshotService snapshotService;

    public FundAdminController(FundFileService fundFileService, SnapshotService snapshotService) {
        this.fundFileService = fundFileService;
        this.snapshotService = snapshotService;
    }

    @GetMapping
    public List<Map<String, String>> listFunds() throws IOException {
        return fundFileService.listFunds();
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getFund(@PathVariable String code) throws IOException {
        return fundFileService.getFund(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncFunds() {
        snapshotService.reloadFundDataAndRebuildSnapshot();
        return ResponseEntity.ok(Map.of("message", "同步成功"));
    }

    @PostMapping
    public ResponseEntity<?> createFund(@RequestBody ObjectNode fundNode) throws IOException {
        if (!fundNode.has("fund_code") || fundNode.get("fund_code").asText().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "fund_code is required"));
        }
        String code = fundNode.get("fund_code").asText();
        if (fundFileService.exists(code)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Fund already exists: " + code));
        }
        fundFileService.saveFund(fundNode);
        snapshotService.reloadFundDataAndRebuildSnapshot();
        return ResponseEntity.status(HttpStatus.CREATED).body(fundNode);
    }

    @PutMapping("/{code}")
    public ResponseEntity<?> updateFund(@PathVariable String code, @RequestBody ObjectNode fundNode)
            throws IOException {
        String bodyCode = fundNode.has("fund_code") ? fundNode.get("fund_code").asText() : "";
        if (!bodyCode.equals(code)) {
            // If body has a different code, rename the file
            if (!bodyCode.isBlank() && !bodyCode.equals(code)) {
                fundFileService.deleteFund(code);
            } else {
                fundNode.put("fund_code", code);
            }
        }
        fundFileService.saveFund(fundNode);
        snapshotService.reloadFundDataAndRebuildSnapshot();
        return ResponseEntity.ok(fundNode);
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteFund(@PathVariable String code) throws IOException {
        try {
            fundFileService.deleteFund(code);
            snapshotService.reloadFundDataAndRebuildSnapshot();
            return ResponseEntity.noContent().build();
        } catch (NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
