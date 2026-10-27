package za.co.example.spending.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import za.co.example.spending.model.Customer;
import za.co.example.spending.repo.CustomerRepository;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import static za.co.example.spending.controller.CustomerController.logger;

@Service
public class CurrentCustomerService {

    private final CustomerRepository customerRepository;

    public CurrentCustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }


    public Customer getCurrentCustomer(
            JwtAuthenticationToken authentication) {

        String email = authentication.getToken().getClaimAsString("email");

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email claim is missing from the access token");
        }

        logger.info("Looking up customer by email: {}", email);

        return customerRepository.findByEmailIgnoreCase(email)
                .map(customer -> {
                    logger.info(
                            "CUSTOMER FOUND: id={}, email={}",
                            customer.getId(),
                            customer.getEmail());
                    return customer;
                })
                .orElseThrow(() -> {
                    logger.error(
                            "CUSTOMER NOT FOUND in database for email: {}",
                            email);
                    return new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "No customer found for the authenticated email");
                });
    }

}
