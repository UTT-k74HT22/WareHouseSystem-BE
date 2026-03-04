package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private RateLimitService rateLimitService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    @DisplayName("Should return 200 when request is valid")
    void shouldReturn200WhenValidRequest() throws Exception {

        PageResponse<InventoryResponse> mockPage =
                PageResponse.<InventoryResponse>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0L)
                        .totalPages(0)
                        .isFirst(true)
                        .isLast(true)
                        .build();

        when(inventoryService.getInventories(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(PageRequest.class)
        )).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/inventories")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "updatedAt")
                        .param("direction", "ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("Should return 400 when sort field invalid")
    void shouldReturn400WhenInvalidSortField() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("sortBy", "abcxyz")
                        .param("direction", "ASC"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when direction invalid")
    void shouldReturn400WhenInvalidDirection() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("sortBy", "updatedAt")
                        .param("direction", "HELLO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when page is negative")
    void shouldReturn400WhenPageNegative() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when size exceeds max limit")
    void shouldReturn400WhenSizeTooLarge() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }
}