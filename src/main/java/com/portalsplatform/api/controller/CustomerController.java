package com.portalsplatform.api.controller;

import com.portalsplatform.api.model.Customer;
import com.portalsplatform.api.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@Tag(name = "Customers", description = "Customer management endpoints")
public class CustomerController {

    private final CustomerRepository customerRepository;

    @Operation(summary = "Create a new customer",
               description = "Creates a new customer in the rewards program")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Customer created successfully"),
        @ApiResponse(responseCode = "400", description = "Bad request - invalid customer data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid API key"),
        @ApiResponse(responseCode = "409", description = "Customer with that ID already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<?> createCustomer(@RequestBody Customer customer) {
        log.info("Creating customer: {}", customer.getCustomerId());

        if (customerRepository.existsByCustomerId(customer.getCustomerId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Customer with that ID already exists");
        }

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer created: {}", savedCustomer.getCustomerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);
    }
}
