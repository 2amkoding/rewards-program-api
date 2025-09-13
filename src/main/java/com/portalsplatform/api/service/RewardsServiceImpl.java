package com.portalsplatform.api.service;

import com.portalsplatform.api.model.Customer;
import com.portalsplatform.api.model.Transaction;
import com.portalsplatform.api.model.dto.RewardsResponse;
import com.portalsplatform.api.repository.CustomerRepository;
import com.portalsplatform.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardsServiceImpl implements RewardsService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public RewardsResponse calculateTotalRewards(String customerId) {
        log.debug("Calculating total rewards for customer: {}", customerId);

        // Verify customer exists
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

        // Get all transactions
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdOrderByTransactionDateDesc(customerId);

        if (transactions.isEmpty()) {
            log.info("No transactions found for customer: {}", customerId);
            return new RewardsResponse(
                    customerId,
                    customer.getFirstName() + " " + customer.getLastName(),
                    0,
                    Map.of(),
                    "No transactions found"
            );
        }

        // Calculate total points
        int totalPoints = transactions.stream()
                .mapToInt(Transaction::getPointsEarned)
                .sum();

        // Group by month
        Map<String, Integer> monthlyPoints = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDate().format(MONTH_FORMATTER),
                        TreeMap::new,  // Keep months sorted
                        Collectors.summingInt(Transaction::getPointsEarned)
                ));

        log.info("Customer {} has {} total points across {} months",
                customerId, totalPoints, monthlyPoints.size());

        return new RewardsResponse(
                customerId,
                customer.getFirstName() + " " + customer.getLastName(),
                totalPoints,
                monthlyPoints,
                "All time"
        );
    }

    @Override
    public RewardsResponse calculateMonthlyRewards(String customerId, String yearMonth) {
        log.debug("Calculating rewards for customer: {} for month: {}", customerId, yearMonth);

        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

        // Parse year-month
        YearMonth ym;
        try {
            ym = YearMonth.parse(yearMonth, MONTH_FORMATTER);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid month format. Use yyyy-MM format: " + yearMonth);
        }

        LocalDateTime startDate = ym.atDay(1).atStartOfDay();
        LocalDateTime endDate = ym.atEndOfMonth().atTime(23, 59, 59);

        // Get transactions for the month
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndTransactionDateBetween(customerId, startDate, endDate);

        int monthPoints = transactions.stream()
                .mapToInt(Transaction::getPointsEarned)
                .sum();

        Map<String, Integer> monthlyBreakdown = Map.of(yearMonth, monthPoints);

        log.info("Customer {} earned {} points in {}", customerId, monthPoints, yearMonth);

        return new RewardsResponse(
                customerId,
                customer.getFirstName() + " " + customer.getLastName(),
                monthPoints,
                monthlyBreakdown,
                "Month: " + yearMonth
        );
    }

    @Override
    public RewardsResponse calculateRewardsForLastMonths(String customerId, int months) {
        log.debug("Calculating rewards for customer: {} for last {} months", customerId, months);

        if (months < 1 || months > 36) {
            throw new IllegalArgumentException("Months must be between 1 and 36");
        }

        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + customerId));

        // Calculate date range
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(months).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);

        // Get transactions
        List<Transaction> transactions = transactionRepository
                .findByCustomerIdAndTransactionDateBetween(customerId, startDate, endDate);

        if (transactions.isEmpty()) {
            log.info("No transactions found for customer {} in last {} months", customerId, months);
            return new RewardsResponse(
                    customerId,
                    customer.getFirstName() + " " + customer.getLastName(),
                    0,
                    Map.of(),
                    String.format("No transactions in last %d months", months)
            );
        }

        // Calculate total and monthly breakdown
        int totalPoints = transactions.stream()
                .mapToInt(Transaction::getPointsEarned)
                .sum();

        Map<String, Integer> monthlyPoints = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getTransactionDate().format(MONTH_FORMATTER),
                        TreeMap::new,
                        Collectors.summingInt(Transaction::getPointsEarned)
                ));

        log.info("Customer {} earned {} points in last {} months", customerId, totalPoints, months);

        return new RewardsResponse(
                customerId,
                customer.getFirstName() + " " + customer.getLastName(),
                totalPoints,
                monthlyPoints,
                String.format("Last %d months", months)
        );
    }
}

