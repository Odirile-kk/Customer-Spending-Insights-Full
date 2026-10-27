package za.co.example.spending.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import za.co.example.spending.model.Customer;
import za.co.example.spending.service.SpendingService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import za.co.example.spending.service.CurrentCustomerService;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/customers/{customerId}")
@Tag(name = "Customer Spending Insights", description = "Dashboard endpoints for customer spending")
public class CustomerController {
    private final SpendingService service;
    private final CurrentCustomerService currentCustomerService;
    public static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(CustomerController.class);

    public CustomerController(SpendingService service, CurrentCustomerService currentCustomerService) {
        this.service = service;
        this.currentCustomerService = currentCustomerService;
    }

    private Customer verifyCustomerAccess(
            String customerId,
            JwtAuthenticationToken authentication) {

        String email = authentication.getToken()
                .getClaimAsString("email");

        logger.info("Authenticated user email: {}", email);

        Customer loggedInCustomer =
                currentCustomerService.getCurrentCustomer(authentication);

        String resolvedCustomerId = loggedInCustomer.getId();

        logger.info("Resolved customer ID: {}", resolvedCustomerId);
        logger.info("Requested customer ID: {}", customerId);

        if (resolvedCustomerId == null
                || !resolvedCustomerId.equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another customer's data"
            );
        }

        return loggedInCustomer;
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(
            @PathVariable String customerId,
            JwtAuthenticationToken authentication) {

        logger.info("PROFILE ENDPOINT HIT: customerId={}", customerId);
        Customer verifiedCustomer;

        if ("current".equalsIgnoreCase(customerId)) {
            verifiedCustomer =
                    currentCustomerService.getCurrentCustomer(authentication);
        } else {
            verifiedCustomer = verifyCustomerAccess(customerId, authentication);
        }

        Customer c = service.customer(verifiedCustomer.getId());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customerId", c.getId());
        m.put("name", c.getName());
        m.put("email", c.getEmail());
        m.put("joinDate", c.getJoinDate());
        m.put("accountType", c.getAccountType());
        m.put("totalSpent", c.getTotalSpent());
        m.put("currency", c.getCurrency());

        return m;
    }

    @GetMapping("/spending/summary")
    @Operation(summary = "Get spending summary for a quick period")
    public Map<String, Object> summary(
            @PathVariable String customerId, JwtAuthenticationToken authentication,
            @RequestParam(defaultValue = "30d") String period) {
        Customer verifiedCustomer = verifyCustomerAccess(customerId, authentication);

        return service.summary(verifiedCustomer.getId(), period);
    }

    @GetMapping("/spending/categories")
    @Operation(summary = "Get spending grouped by category, with optional date filters")
    public Map<String, Object> categories(@PathVariable String customerId, JwtAuthenticationToken authentication,
                                          @RequestParam(defaultValue = "30d") String period,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Customer verifiedCustomer = verifyCustomerAccess(customerId, authentication);
        return service.categories(verifiedCustomer.getId(), period, startDate, endDate);
    }

    @GetMapping("/spending/trends")
    @Operation(summary = "Get monthly spending trends")
    public Map<String, Object> trends(@PathVariable String customerId, JwtAuthenticationToken authentication,
                                      @RequestParam(defaultValue = "12") int months) {
        Customer verifiedCustomer = verifyCustomerAccess(customerId, authentication);
        return service.trends(verifiedCustomer.getId(), months);
    }

    @GetMapping("/transactions")
    @Operation(summary = "List transactions with filters, sorting and pagination")
    public Map<String, Object> transactions(@PathVariable String customerId, JwtAuthenticationToken authentication,
                                            @RequestParam(defaultValue = "20") int limit, @RequestParam(defaultValue = "0") int offset,
                                            @RequestParam(required = false) String category, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, @RequestParam(defaultValue = "date_desc") String sortBy) {
        Customer verifiedCustomer = verifyCustomerAccess(customerId, authentication);

        if (startDate != null && endDate != null && startDate.isAfter(endDate))
            throw new IllegalArgumentException("startDate must be on or before endDate");

        return service.transactionPage(verifiedCustomer.getId(), limit, offset, category, startDate, endDate, sortBy);
    }

    @GetMapping("/goals")
    @Operation(summary = "Get customer spending goals and budget status")
    public Map<String, Object> goals(@PathVariable String customerId, JwtAuthenticationToken authentication) {
        Customer verifiedCustomer = verifyCustomerAccess(customerId, authentication);
        return service.goals(verifiedCustomer.getId());
    }

    @GetMapping("/filters")
    @Operation(summary = "Get available categories and date-range presets")
    public Map<String, Object> filters(@PathVariable String customerId, JwtAuthenticationToken authentication) {
        Customer verifiedCustomer = verifyCustomerAccess(customerId, authentication);
        return service.filters(verifiedCustomer.getId());
    }
}
