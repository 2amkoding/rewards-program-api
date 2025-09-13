package com.portalsplatform.api.service;

import com.portalsplatform.api.model.dto.RewardsResponse;

public interface RewardsService {
    /**
     * Calculate total rewards for a customer across all time
     * @param customerId the customer identifier
     * @return rewards response with total and monthly breakdown
     */
    RewardsResponse calculateTotalRewards(String customerId);

    /**
     * Calculate rewards for a specific month
     * @param customerId the customer identifier
     * @param yearMonth format: "2024-01"
     * @return rewards response for the specified month
     */
    RewardsResponse calculateMonthlyRewards(String customerId, String yearMonth);

    /**
     * Calculate rewards for last N months
     * @param customerId the customer identifier
     * @param months number of months to look back
     * @return rewards response with monthly breakdown
     */
    RewardsResponse calculateRewardsForLastMonths(String customerId, int months);
}


