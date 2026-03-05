package org.demo.whs.controller;

import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.RateLimitService;
import org.demo.whs.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StorageController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandle.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageService storageService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Delete file by query param with nested object path should return 200")
    void should_DeleteFileByQueryParam_When_ObjectNameContainsFolders() throws Exception {
        String objectName = "products/2026/03/file.png";
        doNothing().when(storageService).deleteFile(objectName);

        mockMvc.perform(delete("/api/v1/storage")
                        .param("objectName", objectName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(storageService).deleteFile(objectName);
    }

    @Test
    @DisplayName("Delete file by legacy path wildcard with nested object path should return 200")
    void should_DeleteFileByLegacyPath_When_ObjectNameContainsFolders() throws Exception {
        String objectName = "products/2026/03/file.png";
        doNothing().when(storageService).deleteFile(objectName);

        mockMvc.perform(delete("/api/v1/storage/products/2026/03/file.png"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(storageService).deleteFile(objectName);
    }
}
