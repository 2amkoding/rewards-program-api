package com.portalsplatform.api.controller;

import com.portalsplatform.api.model.Transaction;
import com.portalsplatform.api.repository.CustomerRepository;
import com.portalsplatform.api.repository.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    /**
     * Create a new transaction (for demo/testing)
     * POST /api/transactions
     */
    @Operation(summary = "Create a new transaction",
               description = "Creates a new transaction for a customer and calculates rewards points")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid transaction data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
     * Get all transactions for a customer with pagination
     * GET /api/transactions/customer/{customerId}?page=0&size=20
     */
    @Operation(summary = "Get customer transactions",
               description = "Retrieves paginated list of transactions for a specific customer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved transactions"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Transaction>> getCustomerTransactions(
            @Parameter(description = "Customer ID", example = "CUST001")
            @PathVariable String customerId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        log.info("Fetching transactions for customer: {} (page: {}, size: {})", 
                customerId, page, size);

        if (!customerRepository.existsByCustomerId(customerId)) {
            return ResponseEntity.notFound().build();
        }

        // Limit page size to prevent excessive data retrieval
        int limitedSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, limitedSize, 
                Sort.by(Sort.Direction.DESC, "transactionDate"));

        List<Transaction> transactions = transactionRepository
                .findByCustomerIdOrderByTransactionDateDesc(customerId);

        // For simplicity, we'll return all transactions for now
        // In production, you'd implement paginated repository method
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
