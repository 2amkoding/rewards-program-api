package com.portalsplatform.api.repository;

import com.portalsplatform.api.model.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CustomerRepository extends MongoRepository<Customer, String> {
    // Find customer by business ID (not MongoDB _id)
    Optional<Customer> findByCustomerId(String customerId);

    // Check if customer exists
    boolean existsByCustomerId(String customerId);

    // Delete by business ID
    void deleteByCustomerId(String customerId);
}
