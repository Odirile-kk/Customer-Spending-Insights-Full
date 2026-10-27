package za.co.example.spending.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "spending_goal")
public class SpendingGoal {
    @Id
    private String id;
    private String customerId;
    private String category;
    private BigDecimal monthlyBudget;

    protected SpendingGoal() {
    }

    public SpendingGoal(String id, String customerId, String category, BigDecimal monthlyBudget) {
        this.id = id;
        this.customerId = customerId;
        this.category = category;
        this.monthlyBudget = monthlyBudget;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }
}
