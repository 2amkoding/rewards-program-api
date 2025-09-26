package com.portalsplatform.api.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest extends com.portalsplatform.api.support.AbstractMongoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Inject API key from test configuration (environment variable or fallback)
    @Value("${api.security.api-key}")
    private String validApiKey;

    private static final String INVALID_API_KEY = "definitely-invalid-key";

    @Test
    @DisplayName("Should allow access to health endpoint without API key")
    void shouldAllowHealthEndpointWithoutApiKey() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Should allow access to Swagger UI without API key")
    void shouldAllowSwaggerWithoutApiKey() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should deny access to rewards endpoint without API key")
    void shouldDenyRewardsEndpointWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/customers/CUST001/rewards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or missing API key"));
    }

    @Test
    @DisplayName("Should deny access with invalid API key")
    void shouldDenyAccessWithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/customers/CUST001/rewards")
                        .header("X-API-Key", INVALID_API_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or missing API key"));
    }

    @Test
    @DisplayName("Should allow access with valid API key from configuration")
    void shouldAllowAccessWithValidApiKeyFromConfig() throws Exception {
        // Uses API key injected from test configuration
        mockMvc.perform(get("/api/customers/CUST001/rewards")
                        .header("X-API-Key", validApiKey))
                .andExpect(status().isNotFound()); // 404, not 401 - auth passed
    }

    @Test
    @DisplayName("Should protect transaction endpoints")
    void shouldProtectTransactionEndpoints() throws Exception {
        // Without API key
        mockMvc.perform(get("/api/transactions/customer/CUST001"))
                .andExpect(status().isUnauthorized());

        // With valid API key from config
        mockMvc.perform(get("/api/transactions/customer/CUST001")
                        .header("X-API-Key", validApiKey))
                .andExpect(status().isNotFound()); // Auth passed, customer doesn't exist
    }

    @Test
    @DisplayName("Should validate transaction creation with API key")
    void shouldValidateTransactionCreationWithApiKey() throws Exception {
        String requestBody = """
            {
                "customerId": "CUST001",
                "amount": 100.00,
                "description": "Test purchase"
            }
            """;

        // Without API key
        mockMvc.perform(post("/api/transactions")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        // With valid API key from config
        mockMvc.perform(post("/api/transactions")
                        .header("X-API-Key", validApiKey)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isNotFound()); // Auth passed, customer validation failed
    }
}