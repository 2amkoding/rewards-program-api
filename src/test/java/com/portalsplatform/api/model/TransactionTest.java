package com.portalsplatform.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Transaction Model Tests - Rewards Calculation")
class TransactionTest {

    @ParameterizedTest
    @DisplayName("Should calculate points correctly for various amounts")
    @CsvSource({
            "0.00, 0",      // No points for zero
            "25.00, 0",     // No points under $50
            "49.99, 0",     // No points just under $50
            "50.00, 0",     // No points for exactly $50
            "50.01, 0",     // 1 point for $50.01 (rounds down)
            "51.00, 1",     // 1 point for $51
            "75.00, 25",    // 25 points for $75 (75-50)
            "100.00, 50",   // 50 points for $100 (100-50)
            "100.01, 50",   // 50 points for $100.01 (rounds down)
            "101.00, 52",   // 52 points for $101 (2*1 + 50*1)
            "120.00, 90",   // 90 points for $120 (2*20 + 1*50)
            "200.00, 250",  // 250 points for $200 (2*100 + 1*50)
            "1000.00, 1850" // 1850 points for $1000 (2*900 + 1*50)
    })
    void shouldCalculatePointsCorrectly(String amount, int expectedPoints) {
        // Given
        BigDecimal transactionAmount = new BigDecimal(amount);
        LocalDateTime now = LocalDateTime.now();

        // When
        Transaction transaction = new Transaction(
                "TXN001", "CUST001", transactionAmount, now, "Test Purchase"
        );

        // Then
        assertThat(transaction.getPointsEarned()).isEqualTo(expectedPoints);
    }

    @Test
    @DisplayName("Should handle null amount gracefully")
    void shouldHandleNullAmountGracefully() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        Transaction transaction = new Transaction(
                "TXN001", "CUST001", null, now, "Test Purchase"
        );

        // Then
        assertThat(transaction.getPointsEarned()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle negative amount gracefully")
    void shouldHandleNegativeAmountGracefully() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        Transaction transaction = new Transaction(
                "TXN001", "CUST001", new BigDecimal("-10.00"), now, "Test Purchase"
        );

        // Then
        assertThat(transaction.getPointsEarned()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should set created timestamp automatically")
    void shouldSetCreatedTimestampAutomatically() {
        // Given
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        LocalDateTime transactionTime = LocalDateTime.now();

        // When
        Transaction transaction = new Transaction(
                "TXN001", "CUST001", new BigDecimal("100.00"), transactionTime, "Test"
        );
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        // Then
        assertThat(transaction.getCreatedAt()).isBetween(before, after);
        assertThat(transaction.getTransactionDate()).isEqualTo(transactionTime);
    }
}