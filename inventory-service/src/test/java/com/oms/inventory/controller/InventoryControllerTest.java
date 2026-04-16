package com.oms.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oms.inventory.config.SecurityConfig;
import com.oms.inventory.exception.GlobalExceptionHandler;
import com.oms.inventory.exception.InventoryException;
import com.oms.inventory.service.AdjustStockRequest;
import com.oms.inventory.service.InventoryResponse;
import com.oms.inventory.service.InventoryService;
import com.oms.inventory.service.SetStockRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = InventoryController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = GlobalExceptionHandler.class
    )
)
@Import(SecurityConfig.class)
@DisplayName("InventoryController")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final Instant NOW = Instant.parse("2025-06-01T12:00:00Z");

    @Nested
    @DisplayName("GET /inventory/{productId}")
    class GetStock {

        @Test
        void shouldReturn200WithoutAuthentication() throws Exception {
            var response = new InventoryResponse("prod-1", 10, 2, NOW);
            when(inventoryService.getStock("prod-1")).thenReturn(response);

            mockMvc.perform(get("/inventory/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("prod-1"))
                .andExpect(jsonPath("$.availableQty").value(10))
                .andExpect(jsonPath("$.reservedQty").value(2));
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn200ForCustomerRole() throws Exception {
            var response = new InventoryResponse("prod-1", 5, 0, NOW);
            when(inventoryService.getStock("prod-1")).thenReturn(response);

            mockMvc.perform(get("/inventory/prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQty").value(5));
        }
    }

    @Nested
    @DisplayName("GET /inventory")
    class GetAllStock {

        @Test
        void shouldReturn401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/inventory"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403ForCustomerRole() throws Exception {
            mockMvc.perform(get("/inventory"))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200ForAdminRole() throws Exception {
            var items = List.of(
                new InventoryResponse("prod-1", 10, 2, NOW),
                new InventoryResponse("prod-2", 20, 5, NOW)
            );
            when(inventoryService.getAllStock()).thenReturn(items);

            mockMvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productId").value("prod-1"))
                .andExpect(jsonPath("$[1].productId").value("prod-2"));
        }
    }

    @Nested
    @DisplayName("POST /inventory")
    class SetStock {

        @Test
        void shouldReturn401WhenUnauthenticated() throws Exception {
            var body = objectMapper.writeValueAsString(new SetStockRequest("prod-1", 10));

            mockMvc.perform(post("/inventory")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403ForCustomerRole() throws Exception {
            var body = objectMapper.writeValueAsString(new SetStockRequest("prod-1", 10));

            mockMvc.perform(post("/inventory")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200ForAdminRole() throws Exception {
            var response = new InventoryResponse("prod-1", 10, 0, NOW);
            when(inventoryService.setStock("prod-1", 10)).thenReturn(response);
            var body = objectMapper.writeValueAsString(new SetStockRequest("prod-1", 10));

            mockMvc.perform(post("/inventory")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("prod-1"))
                .andExpect(jsonPath("$.availableQty").value(10));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenProductIdIsBlank() throws Exception {
            var body = """
                {"productId": "", "quantity": 10}
                """;

            mockMvc.perform(post("/inventory")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenQuantityIsNegative() throws Exception {
            var body = """
                {"productId": "prod-1", "quantity": -5}
                """;

            mockMvc.perform(post("/inventory")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200WhenQuantityIsZero() throws Exception {
            var response = new InventoryResponse("prod-1", 0, 0, NOW);
            when(inventoryService.setStock("prod-1", 0)).thenReturn(response);
            var body = """
                {"productId": "prod-1", "quantity": 0}
                """;

            mockMvc.perform(post("/inventory")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQty").value(0));
        }
    }

    @Nested
    @DisplayName("PATCH /inventory/{productId}/adjust")
    class AdjustStock {

        @Test
        void shouldReturn401WhenUnauthenticated() throws Exception {
            var body = """
                {"delta": 5, "reason": "Restock delivery"}
                """;

            mockMvc.perform(patch("/inventory/prod-1/adjust")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "CUSTOMER")
        void shouldReturn403ForCustomerRole() throws Exception {
            var body = """
                {"delta": 5, "reason": "Restock delivery"}
                """;

            mockMvc.perform(patch("/inventory/prod-1/adjust")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn200ForAdminRole() throws Exception {
            var response = new InventoryResponse("prod-1", 15, 2, NOW);
            when(inventoryService.adjustStock("prod-1", 5)).thenReturn(response);
            var body = """
                {"delta": 5, "reason": "Restock delivery"}
                """;

            mockMvc.perform(patch("/inventory/prod-1/adjust")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("prod-1"))
                .andExpect(jsonPath("$.availableQty").value(15));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenReasonIsBlank() throws Exception {
            var body = """
                {"delta": 5, "reason": ""}
                """;

            mockMvc.perform(patch("/inventory/prod-1/adjust")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenReasonIsMissing() throws Exception {
            var body = """
                {"delta": 5}
                """;

            mockMvc.perform(patch("/inventory/prod-1/adjust")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest());
        }
    }
}
