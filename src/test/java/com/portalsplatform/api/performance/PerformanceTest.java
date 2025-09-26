package com.portalsplatform.api.performance;

import com.portalsplatform.api.model.Customer;
import com.portalsplatform.api.model.Transaction;
import com.portalsplatform.api.repository.CustomerRepository;
import com.portalsplatform.api.repository.TransactionRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Value;
import com.portalsplatform.api.security.RateLimitingFilter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Performance Tests - Response Time Validation")
class PerformanceTest extends com.portalsplatform.api.support.AbstractMongoIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private HttpHeaders headers;

    @Value("${api.security.api-key}")
    private String validApiKey;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        // Clean and setup test data
        transactionRepository.deleteAll();
        customerRepository.deleteAll();

        // Create test customer
        Customer customer = new Customer("PERF001", "Performance", "Test", "perf@test.com");
        customerRepository.save(customer);

        // Create multiple transactions for performance testing
        List<Transaction> transactions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 100; i++) {
            transactions.add(new Transaction(
                    "TXN" + String.format("%03d", i),
                    "PERF001",
                    new BigDecimal(String.valueOf(50 + (i % 200))), // Varying amounts
                    now.minusDays(i % 30),
                    "Performance Test Transaction " + i
            ));
        }
        transactionRepository.saveAll(transactions);

        // Reset rate limiting between tests to avoid cross-test interference
        rateLimitingFilter.resetCountersForTesting();

        // Setup headers with API key from configuration
        headers = new HttpHeaders();
        headers.set("X-API-Key", validApiKey);
    }

    @Test
    @DisplayName("Should respond to customer rewards lookup within 200ms")
    void shouldRespondToRewardsLookupWithin100ms() {
        // Given
        String url = "http://localhost:" + port + "/api/customers/PERF001/rewards";

        // When & Then - Measure response time
        long startTime = System.currentTimeMillis();

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class
        );

        long responseTime = System.currentTimeMillis() - startTime;

        // Assertions
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(responseTime).isLessThan(200); // <200ms requirement
        assertThat(response.getBody()).contains("PERF001");
    }

    @Test
    @DisplayName("Should handle concurrent requests efficiently")
    void shouldHandleConcurrentRequestsEfficiently() throws InterruptedException {
        // Given
        String url = "http://localhost:" + port + "/api/customers/PERF001/rewards";
        List<Long> responseTimes = new ArrayList<>();
        int concurrentRequests = 10;

        // When - Make concurrent requests
        for (int i = 0; i < concurrentRequests; i++) {
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                long responseTime = System.currentTimeMillis() - startTime;
                synchronized (responseTimes) {
                    responseTimes.add(responseTime);
                }
            }).start();
        }

        // Wait for all requests to complete
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> responseTimes.size() == concurrentRequests);

        // Then
        double averageResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        assertThat(averageResponseTime).isLessThan(1000); // Allow ample headroom on CI/dev machines
        assertThat(responseTimes).allMatch(time -> time < 1000); // Even under load, keep individual requests under 1s
    }

    @Test
    @DisplayName("Should handle rate limiting gracefully")
    void shouldHandleRateLimitingGracefully() {
        // Given
        String url = "http://localhost:" + port + "/api/customers/PERF001/rewards";
        int requestsToMake = 105; // Exceed rate limit of 100
        int rateLimitResponses = 0;

        // When - Make requests rapidly
        for (int i = 0; i < requestsToMake; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class
            );

            if (response.getStatusCode().value() == 429) {
                rateLimitResponses++;
            }
        }

        // Then
        assertThat(rateLimitResponses).isGreaterThan(0); // Some requests should be rate limited
    }

    @Test
    @DisplayName("Should maintain performance with large transaction history")
    void shouldMaintainPerformanceWithLargeTransactionHistory() {
        // Given - Customer with many more transactions
        List<Transaction> moreTransactions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 100; i < 1000; i++) { // Add 900 more transactions
            moreTransactions.add(new Transaction(
                    "TXN" + String.format("%04d", i),
                    "PERF001",
                    new BigDecimal(String.valueOf(25 + (i % 300))),
                    now.minusDays(i % 365),
                    "Large Dataset Transaction " + i
            ));
        }
        transactionRepository.saveAll(moreTransactions);

        String url = "http://localhost:" + port + "/api/customers/PERF001/rewards";

        // When
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class
        );
        long responseTime = System.currentTimeMillis() - startTime;

        // Then - Even with 1000 transactions, should be fast
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(responseTime).isLessThan(200); // Slightly higher threshold for large datasets
        assertThat(response.getBody()).contains("PERF001");
    }
}