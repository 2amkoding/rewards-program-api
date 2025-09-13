package com.portalsplatform.api.service;

import com.portalsplatform.api.model.Customer;
import com.portalsplatform.api.model.Transaction;
import com.portalsplatform.api.model.dto.RewardsResponse;
import com.portalsplatform.api.repository.CustomerRepository;
import com.portalsplatform.api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardsServiceTest {

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
        testCustomer = new Customer("CUST001", "John", "Doe", "john.doe@test.com");
        
        LocalDateTime now = LocalDateTime.now();
        testTransactions = Arrays.asList(
            createTransaction("TXN001", "CUST001", "120.00", now.minusDays(5)),  // 90 points
            createTransaction("TXN002", "CUST001", "75.00", now.minusDays(15)),  // 25 points
            createTransaction("TXN003", "CUST001", "200.00", now.minusDays(35)), // 250 points
            createTransaction("TXN004", "CUST001", "45.00", now.minusDays(40))   // 0 points
        );
    }

    @Test
    @DisplayName("Should calculate total rewards correctly")
    void calculateTotalRewards_ShouldReturnCorrectTotal() {
        // Given
        when(customerRepository.findByCustomerId("CUST001"))
            .thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findByCustomerIdOrderByTransactionDateDesc("CUST001"))
            .thenReturn(testTransactions);

        // When
        RewardsResponse response = rewardsService.calculateTotalRewards("CUST001");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.customerId()).isEqualTo("CUST001");
        assertThat(response.customerName()).isEqualTo("John Doe");
        assertThat(response.totalPoints()).isEqualTo(365); // 90 + 25 + 250 + 0
        assertThat(response.monthlyPoints()).isNotEmpty();
        
        verify(customerRepository).findByCustomerId("CUST001");
        verify(transactionRepository).findByCustomerIdOrderByTransactionDateDesc("CUST001");
    }

    @Test
    @DisplayName("Should throw exception when customer not found")
    void calculateTotalRewards_CustomerNotFound_ShouldThrowException() {
        // Given
        when(customerRepository.findByCustomerId("INVALID"))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> rewardsService.calculateTotalRewards("INVALID"))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("Customer not found");
    }

    @Test
    @DisplayName("Should handle customer with no transactions")
    void calculateTotalRewards_NoTransactions_ShouldReturnZeroPoints() {
        // Given
        when(customerRepository.findByCustomerId("CUST001"))
            .thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findByCustomerIdOrderByTransactionDateDesc("CUST001"))
            .thenReturn(Collections.emptyList());

        // When
        RewardsResponse response = rewardsService.calculateTotalRewards("CUST001");

        // Then
        assertThat(response.totalPoints()).isEqualTo(0);
        assertThat(response.monthlyPoints()).isEmpty();
        assertThat(response.period()).isEqualTo("No transactions found");
    }

    @Test
    @DisplayName("Should calculate rewards for specific month")
    void calculateMonthlyRewards_ShouldReturnCorrectMonthlyTotal() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endDate = LocalDateTime.now().minusMonths(1).plusMonths(1).withDayOfMonth(1).minusSeconds(1);
        
        when(customerRepository.findByCustomerId("CUST001"))
            .thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findByCustomerIdAndTransactionDateBetween(
            eq("CUST001"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testTransactions.subList(0, 2)); // Return first 2 transactions

        // When
        String lastMonth = LocalDateTime.now().minusMonths(1).format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
        RewardsResponse response = rewardsService.calculateMonthlyRewards("CUST001", lastMonth);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalPoints()).isEqualTo(115); // 90 + 25
    }

    @Test
    @DisplayName("Should validate month format")
    void calculateMonthlyRewards_InvalidFormat_ShouldThrowException() {
        // Given
        when(customerRepository.findByCustomerId("CUST001"))
            .thenReturn(Optional.of(testCustomer));

        // When/Then
        assertThatThrownBy(() -> rewardsService.calculateMonthlyRewards("CUST001", "invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid month format");
    }

    @Test
    @DisplayName("Should calculate rewards for last N months")
    void calculateRewardsForLastMonths_ShouldReturnCorrectPeriodTotal() {
        // Given
        when(customerRepository.findByCustomerId("CUST001"))
            .thenReturn(Optional.of(testCustomer));
        when(transactionRepository.findByCustomerIdAndTransactionDateBetween(
            eq("CUST001"), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(testTransactions);

        // When
        RewardsResponse response = rewardsService.calculateRewardsForLastMonths("CUST001", 3);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.totalPoints()).isEqualTo(365);
        assertThat(response.period()).isEqualTo("Last 3 months");
    }

    @Test
    @DisplayName("Should validate months parameter range")
    void calculateRewardsForLastMonths_InvalidMonths_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> rewardsService.calculateRewardsForLastMonths("CUST001", 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Months must be between 1 and 36");
            
        assertThatThrownBy(() -> rewardsService.calculateRewardsForLastMonths("CUST001", 37))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Months must be between 1 and 36");
    }

    // Helper method
    private Transaction createTransaction(String id, String customerId, String amount, LocalDateTime date) {
        return new Transaction(id, customerId, new BigDecimal(amount), date, "Test transaction");
    }
}
