package com.portalsplatform.api.controller;
import com.portalsplatform.api.model.Transaction;
import com.portalsplatform.api.repository.CustomerRepository;
import com.portalsplatform.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    /**
     * Create a new transaction (for demo/testing)
     * POST /api/transactions
     */
    @PostMapping
    public ResponseEntity<?> createTransaction(@RequestBody TransactionRequest request) {
        log.info("Creating transaction for customer: {} amount: ${}",
                request.customerId(), request.amount());

        // Validate customer exists
        if (!customerRepository.existsByCustomerId(request.customerId())) {
            log.error("Customer not found: {}", request.customerId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Customer not found: " + request.customerId());
        }

        // Validate amount
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                    .body("Amount must be greater than zero");
        }

        // Create transaction
        String transactionId = "TXN" + UUID.randomUUID().toString()
                .substring(0, 8).toUpperCase();

        Transaction transaction = new Transaction(
                transactionId,
                request.customerId(),
                request.amount(),
                LocalDateTime.now(),
                request.description() != null ? request.description() : "Manual transaction"
        );

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created: {} with {} points",
                saved.getTransactionId(), saved.getPointsEarned());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Get all transactions for a customer
     * GET /api/transactions/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Transaction>> getCustomerTransactions(
            @PathVariable String customerId) {

        log.info("Fetching transactions for customer: {}", customerId);

        if (!customerRepository.existsByCustomerId(customerId)) {
            return ResponseEntity.notFound().build();
        }

        List<Transaction> transactions = transactionRepository
                .findByCustomerIdOrderByTransactionDateDesc(customerId);

        log.info("Found {} transactions for customer {}",
                transactions.size(), customerId);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Request DTO for creating transactions
     */
    public record TransactionRequest(
            String customerId,
            BigDecimal amount,
            String description
    ) {}
}
