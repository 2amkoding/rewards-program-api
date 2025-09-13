package com.portalsplatform.api.controller;

import com.portalsplatform.api.model.dto.RewardsResponse;
import com.portalsplatform.api.service.RewardsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") //Configure appropriately for production
public class RewardsController {
    private final RewardsService rewardsService;

    /**
     * Get total rewards for a customer
     * GET /api/customers/{customerId}/rewards
     */
    @GetMapping("/{customerId}/rewards")
    public ResponseEntity<RewardsResponse> getTotalRewards(
            @PathVariable String customerId) {

        log.info("Request received: GET /api/customers/{}/rewards", customerId);

        try {
            RewardsResponse response = rewardsService.calculateTotalRewards(customerId);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            log.error("Customer not found: {}", customerId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error calculating rewards for customer: {}", customerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get rewards for a specific month
     * GET /api/customers/{customerId}/rewards/{month}
     * Month format: "2024-01"
     */
    @GetMapping("/{customerId}/rewards/{month}")
    public ResponseEntity<RewardsResponse> getMonthlyRewards(
            @PathVariable String customerId,
            @PathVariable String month) {

        log.info("Request received: GET /api/customers/{}/rewards/{}", customerId, month);

        try {
            // Basic format validation
            if (!month.matches("\\d{4}-\\d{2}")) {
                log.error("Invalid month format: {}", month);
                return ResponseEntity.badRequest().build();
            }

            RewardsResponse response = rewardsService.calculateMonthlyRewards(customerId, month);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            log.error("Customer not found: {}", customerId);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error calculating monthly rewards", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get rewards for last N months
     * GET /api/customers/{customerId}/rewards/recent?months=3
     */
    @GetMapping("/{customerId}/rewards/recent")
    public ResponseEntity<RewardsResponse> getRecentRewards(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "3") int months) {

        log.info("Request received: GET /api/customers/{}/rewards/recent?months={}",
                customerId, months);

        try {
            RewardsResponse response = rewardsService.calculateRewardsForLastMonths(customerId, months);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            log.error("Customer not found: {}", customerId);
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            log.error("Invalid months parameter: {}", months);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error calculating recent rewards", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
