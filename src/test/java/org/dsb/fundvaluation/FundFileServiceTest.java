package org.dsb.fundvaluation;

import org.dsb.fundvaluation.service.FundFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FundFileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void listFundsIncludesAdminUiFieldNames() throws Exception {
        Files.writeString(tempDir.resolve("000001.json"), """
                {"fund_code":"000001","fund_name":"Test Fund","holdings":[]}
                """);

        FundFileService service = new FundFileService(new ObjectMapper(), tempDir.toString());

        List<Map<String, String>> funds = service.listFunds();

        assertThat(funds).hasSize(1);
        assertThat(funds.get(0))
                .containsEntry("code", "000001")
                .containsEntry("name", "Test Fund")
                .containsEntry("fund_code", "000001")
                .containsEntry("fund_name", "Test Fund")
                .containsEntry("file_name", "000001.json");
    }
}
