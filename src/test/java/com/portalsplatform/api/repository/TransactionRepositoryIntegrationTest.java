package com.portalsplatform.api.repository;

import com.portalsplatform.api.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataMongoTest
@Testcontainers
@DisplayName("Transaction Repository Integration Tests")
class TransactionRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:7")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
    }

    @Autowired
    private TransactionRepository transactionRepository;

    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        baseTime = LocalDateTime.now();

        // Create test data
        List<Transaction> testTransactions = List.of(
                new Transaction("TXN001", "CUST001", new BigDecimal("120.00"),
                        baseTime.minusDays(5), "Electronics"),
                new Transaction("TXN002", "CUST001", new BigDecimal("75.00"),
                        baseTime.minusDays(15), "Grocery"),
                new Transaction("TXN003", "CUST002", new BigDecimal("200.00"),
                        baseTime.minusDays(10), "Furniture"),
                new Transaction("TXN004", "CUST001", new BigDecimal("45.00"),
                        baseTime.minusDays(35), "Gas")
        );

        transactionRepository.saveAll(testTransactions);
    }

    @Test
    @DisplayName("Should find transactions by customer ID ordered by date desc")
    void shouldFindTransactionsByCustomerIdOrderedByDateDesc() {
        // When
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdOrderByTransactionDateDesc("CUST001");

        // Then
        assertThat(transactions).hasSize(3);
        assertThat(transactions.get(0).getDescription()).isEqualTo("Electronics"); // Most recent
        assertThat(transactions.get(1).getDescription()).isEqualTo("Grocery");
        assertThat(transactions.get(2).getDescription()).isEqualTo("Gas"); // Oldest
    }

    @Test
    @DisplayName("Should find transactions by customer and date range")
    void shouldFindTransactionsByCustomerAndDateRange() {
        // Given
        LocalDateTime startDate = baseTime.minusDays(20);
        LocalDateTime endDate = baseTime.minusDays(1);

        // When
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndTransactionDateBetween("CUST001", startDate, endDate);

        // Then
        assertThat(transactions).hasSize(2); // Electronics and Grocery, but not Gas
        assertThat(transactions)
                .extracting(Transaction::getDescription)
                .containsExactlyInAnyOrder("Electronics", "Grocery");
    }

    @Test
    @DisplayName("Should return empty list for non-existent customer")
    void shouldReturnEmptyListForNonExistentCustomer() {
        // When
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdOrderByTransactionDateDesc("NONEXISTENT");

        // Then
        assertThat(transactions).isEmpty();
    }

    @Test
    @DisplayName("Should persist transaction with calculated points")
    void shouldPersistTransactionWithCalculatedPoints() {
        // Given
        Transaction newTransaction = new Transaction(
                "TXN999", "CUST999", new BigDecimal("150.00"),
                LocalDateTime.now(), "Test Purchase"
        );

        // When
        Transaction saved = transactionRepository.save(newTransaction);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPointsEarned()).isEqualTo(150); // 2*50 + 1*50
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}