package za.co.example.spending.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "spending_transaction")
public class SpendingTransaction {
    @Id
    private String id;
    private String customerId;
    private Instant date;
    private String merchant;
    private String category;
    private BigDecimal amount;
    private String description;
    private String paymentMethod;

    protected SpendingTransaction() {
    }

    public SpendingTransaction(String id, String customerId, Instant date, String merchant, String category, BigDecimal amount, String description, String paymentMethod) {
        this.id = id;
        this.customerId = customerId;
        this.date = date;
        this.merchant = merchant;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.paymentMethod = paymentMethod;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Instant getDate() {
        return date;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}
