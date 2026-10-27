package za.co.example.spending.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
public class Customer {
    @Id
    private String id;
    private String accountId;
    private String name;
    private String email;
    private LocalDate joinDate;
    private String accountType;
    private BigDecimal totalSpent;
    private String currency;

    protected Customer() {
    }

    public Customer(
            String id,
            String accountId,
            String name,
            String email,
            LocalDate joinDate,
            String accountType,
            BigDecimal totalSpent,
            String currency) {
        this.id = id;
        this.accountId = accountId;
        this.name = name;
        this.email = email;
        this.joinDate = joinDate;
        this.accountType = accountType;
        this.totalSpent = totalSpent;
        this.currency = currency;
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public String getAccountType() {
        return accountType;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public String getCurrency() {
        return currency;
    }
}
