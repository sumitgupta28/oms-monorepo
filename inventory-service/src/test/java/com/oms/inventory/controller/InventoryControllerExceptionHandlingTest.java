package com.oms.inventory.controller;

import com.oms.inventory.config.SecurityConfig;
import com.oms.inventory.exception.GlobalExceptionHandler;
import com.oms.inventory.exception.InventoryException;
import com.oms.inventory.service.InventoryResponse;
import com.oms.inventory.service.InventoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("InventoryController exception handling")
class InventoryControllerExceptionHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn422ForInventoryException() throws Exception {
        when(inventoryService.adjustStock(anyString(), anyInt()))
            .thenThrow(new InventoryException("Product not found: prod-999"));
        var body = """
            {"delta": 5, "reason": "Restock"}
            """;

        mockMvc.perform(patch("/inventory/prod-999/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Inventory Error"))
            .andExpect(jsonPath("$.detail").value("Product not found: prod-999"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn422ForValidationErrorOnSetStock() throws Exception {
        var body = """
            {"productId": "", "quantity": 10}
            """;

        mockMvc.perform(post("/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Validation Error"))
            .andExpect(jsonPath("$.detail").value("Validation failed"))
            .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn422ForValidationErrorOnAdjustWithBlankReason() throws Exception {
        var body = """
            {"delta": 5, "reason": ""}
            """;

        mockMvc.perform(patch("/inventory/prod-1/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn422ForNegativeQuantityOnSetStock() throws Exception {
        var body = """
            {"productId": "prod-1", "quantity": -5}
            """;

        mockMvc.perform(post("/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Validation Error"));
    }
}
