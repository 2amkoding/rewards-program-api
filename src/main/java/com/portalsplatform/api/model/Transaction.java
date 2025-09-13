package com.portalsplatform.api.model;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "customer_date_idx", def = "{'customerId': 1, 'transactionDate': -1}"),
        @CompoundIndex(name = "date_customer_idx", def = "{'transactionDate': -1, 'customerId': 1}"),
        @CompoundIndex(name = "rewards_calc_idx", def = "{'customerId': 1, 'transactionDate': 1, 'amount': 1}")
})
public class Transaction {

    @Id
    private String id;  // MongoDB generates this

    private String transactionId;  // Business identifier

    private String customerId;  // Links to Customer.customerId

    private BigDecimal amount;  // Using BigDecimal for money

    private LocalDateTime transactionDate;

    private String description;

    private Integer pointsEarned;  // Calculated points for this transaction

    private LocalDateTime createdAt;

    // Constructor for creating new transactions
    public Transaction(String transactionId, String customerId, BigDecimal amount,
                       LocalDateTime transactionDate, String description) {
        this.transactionId = transactionId;
        this.customerId = customerId;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.description = description;
        this.pointsEarned = calculatePoints(amount);
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Calculate rewards points based on transaction amount
     * - 2 points per dollar spent over $100
     * - 1 point per dollar spent between $50-$100
     * - 0 points for amounts under $50
     *
     * Example: $120 = 2×20 + 1×50 = 90 points
     */
    private Integer calculatePoints(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        int amountInt = amount.intValue();  // Round down to nearest dollar
        int points = 0;

        if (amountInt > 100) {
            points += (amountInt - 100) * 2;  // 2 points per dollar over $100
            points += 50;  // 1 point per dollar from $50-$100
        } else if (amountInt > 50) {
            points += (amountInt - 50);  // 1 point per dollar from $50-$100
        }

        return points;
    }
}