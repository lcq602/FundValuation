package org.dsb.fundvaluation;

import org.dsb.fundvaluation.controller.FundAdminController;
import org.dsb.fundvaluation.service.FundFileService;
import org.dsb.fundvaluation.service.SnapshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FundAdminController.class)
class FundAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FundFileService fundFileService;

    @MockitoBean
    private SnapshotService snapshotService;

    @Test
    void createFundReloadsSnapshotAfterSavingFile() throws Exception {
        when(fundFileService.exists("000001")).thenReturn(false);

        mockMvc.perform(post("/api/admin/funds")
                        .contentType("application/json")
                        .content("""
                                {"fund_code":"000001","fund_name":"Test Fund","holdings":[]}
                                """))
                .andExpect(status().isCreated());

        verify(fundFileService).saveFund(any());
        verify(snapshotService).reloadFundDataAndRebuildSnapshot();
    }

    @Test
    void deleteFundReloadsSnapshotAfterDeletingFile() throws Exception {
        mockMvc.perform(delete("/api/admin/funds/000001"))
                .andExpect(status().isNoContent());

        verify(fundFileService).deleteFund("000001");
        verify(snapshotService).reloadFundDataAndRebuildSnapshot();
    }

    @Test
    void syncFundsReloadsSnapshotOnDemand() throws Exception {
        mockMvc.perform(post("/api/admin/funds/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("同步成功"));

        verify(snapshotService).reloadFundDataAndRebuildSnapshot();
    }
}
