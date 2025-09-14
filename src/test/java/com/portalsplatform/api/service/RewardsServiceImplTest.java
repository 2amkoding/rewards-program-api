package com.portalsplatform.api.service;

import com.portalsplatform.api.model.Customer;
import com.portalsplatform.api.model.Transaction;
import com.portalsplatform.api.model.dto.RewardsResponse;
import com.portalsplatform.api.repository.CustomerRepository;
import com.portalsplatform.api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewardsService Unit Tests")
class RewardsServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RewardsServiceImpl rewardsService;

    private Customer testCustomer;
    private List<Transaction> testTransactions;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer("CUST001", "John", "Doe", "john@example.com");

        LocalDateTime now = LocalDateTime.now();
        testTransactions = Arrays.asList(
                new Transaction("TXN001", "CUST001", new BigDecimal("120.00"),
                        now.minusDays(5), "Electronics Store"),  // 90 points
                new Transaction("TXN002", "CUST001", new BigDecimal("75.00"),
                        now.minusDays(15), "Grocery Store"),     // 25 points
                new Transaction("TXN003", "CUST001", new BigDecimal("45.00"),
                        now.minusDays(25), "Gas Station")        // 0 points
        );
    }

    @Test
    @DisplayName("Should calculate total rewards correctly")
    void shouldCalculateTotalRewardsCorrectly() {
        // Given
        when(customerRepository.findByCustomerId("CUST001")).thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findByCustomerIdOrderByTransactionDateDesc("CUST001"))
                .thenReturn(testTransactions);

        // When
        RewardsResponse response = rewardsService.calculateTotalRewards("CUST001");

        // Then
        assertThat(response.getCustomerId()).isEqualTo("CUST001");
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
        assertThat(response.getTotalPoints()).isEqualTo(115); // 90 + 25 + 0
        assertThat(response.getPeriodDescription()).isEqualTo("All time");

        verify(customerRepository).findByCustomerId("CUST001");
        verify(transactionRepository).findByCustomerIdOrderByTransactionDateDesc("CUST001");
    }

    @Test
    @DisplayName("Should throw exception for non-existent customer")
    void shouldThrowExceptionForNonExistentCustomer() {
        // Given
        when(customerRepository.findByCustomerId("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> rewardsService.calculateTotalRewards("INVALID"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Customer not found: INVALID");
    }

    @Test
    @DisplayName("Should handle customer with no transactions")
    void shouldHandleCustomerWithNoTransactions() {
        // Given
        when(customerRepository.findByCustomerId("CUST001")).thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findByCustomerIdOrderByTransactionDateDesc("CUST001"))
                .thenReturn(Arrays.asList());

        // When
        RewardsResponse response = rewardsService.calculateTotalRewards("CUST001");

        // Then
        assertThat(response.getTotalPoints()).isEqualTo(0);
        assertThat(response.getMonthlyPoints()).isEmpty();
        assertThat(response.getPeriodDescription()).isEqualTo("No transactions found");
    }

    @Test
    @DisplayName("Should calculate monthly rewards correctly")
    void shouldCalculateMonthlyRewardsCorrectly() {
        // Given
        String yearMonth = "2024-09";
        LocalDateTime startDate = LocalDateTime.of(2024, 9, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 9, 30, 23, 59, 59);

        List<Transaction> monthlyTransactions = Arrays.asList(
                new Transaction("TXN001", "CUST001", new BigDecimal("120.00"),
                        LocalDateTime.of(2024, 9, 15, 10, 0), "Store Purchase")
        );

        when(customerRepository.findByCustomerId("CUST001")).thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findByCustomerIdAndTransactionDateBetween(
                eq("CUST001"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(monthlyTransactions);

        // When
        RewardsResponse response = rewardsService.calculateMonthlyRewards("CUST001", yearMonth);

        // Then
        assertThat(response.getTotalPoints()).isEqualTo(90); // 2*20 + 1*50
        assertThat(response.getMonthlyPoints()).containsEntry(yearMonth, 90);
        assertThat(response.getPeriodDescription()).isEqualTo("Month: " + yearMonth);
    }

    @Test
    @DisplayName("Should validate recent rewards calculation")
    void shouldValidateRecentRewardsCalculation() {
        // Given
        int months = 3;
        when(customerRepository.findByCustomerId("CUST001")).thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findByCustomerIdAndTransactionDateBetween(
                eq("CUST001"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(testTransactions);

        // When
        RewardsResponse response = rewardsService.calculateRewardsForLastMonths("CUST001", months);

        // Then
        assertThat(response.getTotalPoints()).isEqualTo(115);
        assertThat(response.getPeriodDescription()).isEqualTo("Last 3 months");
    }

    @Test
    @DisplayName("Should validate months parameter bounds")
    void shouldValidateMonthsParameterBounds() {
        // When & Then - Test lower bound
        assertThatThrownBy(() -> rewardsService.calculateRewardsForLastMonths("CUST001", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Months must be between 1 and 36");

        // Test upper bound
        assertThatThrownBy(() -> rewardsService.calculateRewardsForLastMonths("CUST001", 37))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Months must be between 1 and 36");
    }
}