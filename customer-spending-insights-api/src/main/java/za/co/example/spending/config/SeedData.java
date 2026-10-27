
package za.co.example.spending.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import za.co.example.spending.model.Customer;
import za.co.example.spending.model.SpendingGoal;
import za.co.example.spending.model.SpendingTransaction;
import za.co.example.spending.repo.CustomerRepository;
import za.co.example.spending.repo.GoalRepository;
import za.co.example.spending.repo.TransactionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SeedData {

    public static final Map<String, String[]> CATEGORY_META =
            new LinkedHashMap<>();

    static {
        CATEGORY_META.put("Groceries",
                new String[]{"#FF6B6B", "shopping-cart"});
        CATEGORY_META.put("Entertainment",
                new String[]{"#4ECDC4", "film"});
        CATEGORY_META.put("Transportation",
                new String[]{"#45B7D1", "car"});
        CATEGORY_META.put("Dining",
                new String[]{"#F7DC6F", "utensils"});
        CATEGORY_META.put("Shopping",
                new String[]{"#BB8FCE", "shopping-bag"});
        CATEGORY_META.put("Utilities",
                new String[]{"#85C1E9", "zap"});
    }

    private static final String CUSTOMER_ID = "12345";
    private static final String ACCOUNT_ID = "12345";

    @Bean
    CommandLineRunner seed(
            CustomerRepository customers,
            TransactionRepository transactions,
            GoalRepository goals) {

        return args -> {
            transactions.deleteAll();
            goals.deleteAll();
            customers.deleteAll();

            Customer john = new Customer(
                    CUSTOMER_ID,
                    ACCOUNT_ID,
                    "John Doe",
                    "john.doe@email.com",
                    LocalDate.of(2023, 1, 15),
                    "premium",
                    new BigDecimal("15420.50"),
                    "ZAR"
            );

            customers.save(john);

            List<SpendingTransaction> johnTransactions =
                    new ArrayList<>();

            johnTransactions.add(new SpendingTransaction(
                    "txn_123456",
                    CUSTOMER_ID,
                    Instant.parse("2024-09-16T14:30:00Z"),
                    "Pick n Pay",
                    "Groceries",
                    new BigDecimal("245.80"),
                    "Weekly groceries",
                    "Credit Card"
            ));

            johnTransactions.add(new SpendingTransaction(
                    "txn_123457",
                    CUSTOMER_ID,
                    Instant.parse("2024-09-15T10:15:00Z"),
                    "Netflix",
                    "Entertainment",
                    new BigDecimal("199.00"),
                    "Monthly subscription",
                    "Debit Order"
            ));

            transactions.saveAll(johnTransactions);

            List<SpendingGoal> johnGoals = List.of(
                    new SpendingGoal(
                            "goal_001",
                            CUSTOMER_ID,
                            "Entertainment",
                            new BigDecimal("1000.00")
                    ),
                    new SpendingGoal(
                            "goal_002",
                            CUSTOMER_ID,
                            "Groceries",
                            new BigDecimal("1500.00")
                    )
            );

            goals.saveAll(johnGoals);

            System.out.println(
                    "Seed data created successfully for John Doe."
            );
            System.out.println(
                    "Customer ID: " + CUSTOMER_ID
            );
            System.out.println(
                    "Transactions seeded: " + johnTransactions.size()
            );
            System.out.println(
                    "Spending goals seeded: " + johnGoals.size()
            );
        };
    }
}
