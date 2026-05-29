package org.dsb.fundvaluation;

import org.dsb.fundvaluation.service.FundDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FundDataServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFundsPrefersExternalDataDirectory() throws Exception {
        Files.writeString(tempDir.resolve("000001.json"), """
                {
                  "fund_code": "000001",
                  "fund_name": "External Fund",
                  "holdings": [
                    {"stock_code": "00700", "stock_name": "Tencent", "ratio": 0.1}
                  ]
                }
                """);

        var resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources("classpath:funds/*.json")).thenReturn(new Resource[0]);

        var service = new FundDataService(new ObjectMapper(), resolver, tempDir.toString());
        service.loadFunds();

        assertThat(service.getAllFunds()).hasSize(1);
        assertThat(service.getAllFunds().get(0).getFundCode()).isEqualTo("000001");
        assertThat(service.getAllFunds().get(0).getFundName()).isEqualTo("External Fund");
        assertThat(service.getAllFunds().get(0).getFileName()).isEqualTo("000001.json");
    }

    @Test
    void reloadIfChangedOnlyReloadsWhenExternalFilesChange() throws Exception {
        Path fundFile = tempDir.resolve("000001.json");
        Files.writeString(fundFile, """
                {"fund_code":"000001","fund_name":"Original Fund","holdings":[]}
                """);

        var resolver = mock(ResourcePatternResolver.class);
        when(resolver.getResources("classpath:funds/*.json")).thenReturn(new Resource[0]);

        var service = new FundDataService(new ObjectMapper(), resolver, tempDir.toString());
        service.loadFunds();

        assertThat(service.reloadIfChanged()).isFalse();

        Thread.sleep(5);
        Files.writeString(fundFile, """
                {"fund_code":"000001","fund_name":"Changed Fund","holdings":[]}
                """);

        assertThat(service.reloadIfChanged()).isTrue();
        assertThat(service.getAllFunds().get(0).getFundName()).isEqualTo("Changed Fund");
    }
}
