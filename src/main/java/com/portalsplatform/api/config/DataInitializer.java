package com.portalsplatform.api.config;

import com.portalsplatform.api.model.Customer;
import com.portalsplatform.api.model.Transaction;
import com.portalsplatform.api.repository.CustomerRepository;
import com.portalsplatform.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    @Bean
    @Profile("!test") // Don't run during tests
    CommandLineRunner initDatabase() {
        return args -> {
            // Check if data already exists
            if (customerRepository.count() > 0) {
                log.info("Database already initialized with {} customers",
                        customerRepository.count());
                return;
            }

            log.info("========================================");
            log.info("Initializing database with sample data...");
            log.info("========================================");

            // Create sample customers
            Customer customer1 = new Customer("CUST001", "John", "Doe", "john.doe@example.com");
            Customer customer2 = new Customer("CUST002", "Jane", "Smith", "jane.smith@example.com");
            Customer customer3 = new Customer("CUST003", "Bob", "Johnson", "bob.johnson@example.com");

            List<Customer> customers = Arrays.asList(customer1, customer2, customer3);
            customerRepository.saveAll(customers);
            log.info("✅ Created {} customers", customers.size());

            // Create sample transactions for testing rewards calculation
            LocalDateTime now = LocalDateTime.now();

            List<Transaction> transactions = Arrays.asList(
                    // Customer 1 - John Doe transactions (varied amounts)
                    new Transaction("TXN001", "CUST001", new BigDecimal("120.00"),
                            now.minusDays(5), "Electronics Store"),  // 90 points (20*2 + 50*1)
                    new Transaction("TXN002", "CUST001", new BigDecimal("75.00"),
                            now.minusDays(15), "Grocery Store"),     // 25 points (25*1)
                    new Transaction("TXN003", "CUST001", new BigDecimal("200.00"),
                            now.minusDays(35), "Department Store"),  // 250 points (100*2 + 50*1)
                    new Transaction("TXN004", "CUST001", new BigDecimal("45.00"),
                            now.minusDays(40), "Gas Station"),       // 0 points (under $50)
                    new Transaction("TXN005", "CUST001", new BigDecimal("51.00"),
                            now.minusDays(65), "Restaurant"),        // 1 point (1*1)

                    // Customer 2 - Jane Smith transactions
                    new Transaction("TXN006", "CUST002", new BigDecimal("90.00"),
                            now.minusDays(10), "Online Shopping"),   // 40 points (40*1)
                    new Transaction("TXN007", "CUST002", new BigDecimal("150.00"),
                            now.minusDays(25), "Home Improvement"),  // 150 points (50*2 + 50*1)
                    new Transaction("TXN008", "CUST002", new BigDecimal("50.00"),
                            now.minusDays(60), "Bookstore"),         // 0 points (exactly $50)

                    // Customer 3 - Bob Johnson transactions
                    new Transaction("TXN009", "CUST003", new BigDecimal("300.00"),
                            now.minusDays(5), "Furniture Store"),    // 450 points (200*2 + 50*1)
                    new Transaction("TXN010", "CUST003", new BigDecimal("25.00"),
                            now.minusDays(30), "Coffee Shop"),       // 0 points (under $50)
                    new Transaction("TXN011", "CUST003", new BigDecimal("100.00"),
                            now.minusDays(45), "Clothing Store")     // 50 points (0*2 + 50*1)
            );

            transactionRepository.saveAll(transactions);
            log.info("✅ Created {} transactions", transactions.size());

            // Display summary
            log.info("========================================");
            log.info("Sample Data Summary:");
            log.info("========================================");

            for (Customer customer : customers) {
                List<Transaction> custTransactions =
                        transactionRepository.findByCustomerIdOrderByTransactionDateDesc(customer.getCustomerId());

                int totalPoints = custTransactions.stream()
                        .mapToInt(Transaction::getPointsEarned)
                        .sum();

                log.info("Customer: {} {} ({})",
                        customer.getFirstName(),
                        customer.getLastName(),
                        customer.getCustomerId());
                log.info("  - Transactions: {}", custTransactions.size());
                log.info("  - Total Points: {}", totalPoints);

                for (Transaction t : custTransactions) {
                    log.info("    * {} - ${} = {} points",
                            t.getDescription(),
                            t.getAmount(),
                            t.getPointsEarned());
                }
            }

            log.info("========================================");
            log.info("✅ Database initialization complete!");
            log.info("========================================");
        };
    }
}
