package com.portalsplatform.api.controller;

import com.portalsplatform.api.model.dto.RewardsResponse;
import com.portalsplatform.api.service.RewardsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Rewards", description = "Customer rewards calculation endpoints")
public class RewardsController {
    private final RewardsService rewardsService;

    /**
     * Get total rewards for a customer
     * GET /api/customers/{customerId}/rewards
     */
    @Operation(summary = "Get total rewards for a customer",
               description = "Calculates and returns all-time rewards points for a specific customer")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully calculated rewards"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{customerId}/rewards")
    public ResponseEntity<RewardsResponse> getTotalRewards(
            @Parameter(description = "Customer ID", example = "CUST001")
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
    @Operation(summary = "Get monthly rewards for a customer",
               description = "Calculates and returns rewards points for a specific month")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully calculated monthly rewards"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid month format"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{customerId}/rewards/{month}")
    public ResponseEntity<RewardsResponse> getMonthlyRewards(
            @Parameter(description = "Customer ID", example = "CUST001")
            @PathVariable String customerId,
            @Parameter(description = "Month in YYYY-MM format", example = "2024-09")
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
    @Operation(summary = "Get recent rewards for a customer",
               description = "Calculates and returns rewards points for the last N months")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully calculated recent rewards"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid months parameter"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key"),
        @ApiResponse(responseCode = "404", description = "Customer not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{customerId}/rewards/recent")
    public ResponseEntity<RewardsResponse> getRecentRewards(
            @Parameter(description = "Customer ID", example = "CUST001")
            @PathVariable String customerId,
            @Parameter(description = "Number of months to look back (1-36)", example = "3")
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
