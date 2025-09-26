package com.portalsplatform.api.controller;

import com.portalsplatform.api.model.dto.RewardsResponse;
import com.portalsplatform.api.service.RewardsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import com.portalsplatform.api.security.JsonSchemaValidationFilter;
import com.portalsplatform.api.security.ApiKeyAuthFilter;
import com.portalsplatform.api.security.RateLimitingFilter;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(RewardsController.class)
@AutoConfigureMockMvc(addFilters = false)
class RewardsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Disable custom filters by mocking them in slice tests
    @MockBean
    private JsonSchemaValidationFilter jsonSchemaValidationFilter;

    @MockBean
    private ApiKeyAuthFilter apiKeyAuthFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @MockBean
    private RewardsService rewardsService;

    @Test
    void getTotalRewards_ShouldReturnOk() throws Exception {
        // Given
        RewardsResponse mockResponse = new RewardsResponse(
            "CUST001",
            "John Doe",
            365,
            Map.of("2024-09", 365),
            "All time"
        );
        when(rewardsService.calculateTotalRewards("CUST001")).thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(get("/api/customers/CUST001/rewards"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value("CUST001"))
            .andExpect(jsonPath("$.totalPoints").value(365))
            .andExpect(jsonPath("$.customerName").value("John Doe"));
    }

    @Test
    void getTotalRewards_CustomerNotFound_ShouldReturn404() throws Exception {
        // Given
        when(rewardsService.calculateTotalRewards("INVALID"))
            .thenThrow(new NoSuchElementException("Customer not found"));

        // When/Then
        mockMvc.perform(get("/api/customers/INVALID/rewards"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getMonthlyRewards_ValidMonth_ShouldReturnOk() throws Exception {
        // Given
        RewardsResponse mockResponse = new RewardsResponse(
            "CUST001",
            "John Doe",
            115,
            Map.of("2024-09", 115),
            "Month: 2024-09"
        );
        when(rewardsService.calculateMonthlyRewards("CUST001", "2024-09"))
            .thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(get("/api/customers/CUST001/rewards/2024-09"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPoints").value(115));
    }

    @Test
    void getMonthlyRewards_InvalidFormat_ShouldReturn400() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/customers/CUST001/rewards/invalid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getRecentRewards_ShouldReturnOk() throws Exception {
        // Given
        RewardsResponse mockResponse = new RewardsResponse(
            "CUST001",
            "John Doe",
            200,
            Map.of("2024-09", 100, "2024-08", 100),
            "Last 3 months"
        );
        when(rewardsService.calculateRewardsForLastMonths("CUST001", 3))
            .thenReturn(mockResponse);

        // When/Then
        mockMvc.perform(get("/api/customers/CUST001/rewards/recent?months=3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.period").value("Last 3 months"));
    }
}
