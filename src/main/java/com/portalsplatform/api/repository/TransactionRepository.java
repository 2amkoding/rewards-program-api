package com.portalsplatform.api.repository;

import com.portalsplatform.api.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    // Find all transactions for a customer, newest first
    List<Transaction> findByCustomerIdOrderByTransactionDateDesc(String customerId);

    // Find transactions in date range
    List<Transaction> findByCustomerIdAndTransactionDateBetween(
            String customerId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Custom query for monthly calculations
    @Query("{'customerId': ?0, 'transactionDate': {$gte: ?1, $lte: ?2}}")
    List<Transaction> findCustomerTransactionsInPeriod(
            String customerId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    // Count transactions for a customer
    long countByCustomerId(String customerId);
}
