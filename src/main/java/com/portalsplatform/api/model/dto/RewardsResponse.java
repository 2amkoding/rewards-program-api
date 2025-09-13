package com.portalsplatform.api.model.dto;

import java.util.Map;

/**
 * DTO for rewards API responses.
 * Using record for immutability and reduced boilerplate.
 *
 * @param customerId unique customer identifier
 * @param customerName full name of customer
 * @param totalPoints total rewards points across all transactions
 * @param monthlyPoints map of month to points (e.g., "2024-01": 150)
 * @param period description of time period (e.g., "Last 3 months")
 */
public record RewardsResponse(
        String customerId,
        String customerName,
        Integer totalPoints,
        Map<String, Integer> monthlyPoints,
        String period
) {
    // Compact constructor for validation and defensive copying
    public RewardsResponse {
        // Make monthlyPoints immutable
        monthlyPoints = monthlyPoints != null ?
                Map.copyOf(monthlyPoints) : Map.of();
    }

    // Static factory method for common use case
    public static RewardsResponse empty(String customerId, String customerName) {
        return new RewardsResponse(
                customerId,
                customerName,
                0,
                Map.of(),
                "No transactions"
        );
    }
}
