package za.co.example.spending.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.example.spending.model.SpendingGoal;

import java.util.List;

public interface GoalRepository extends JpaRepository<SpendingGoal, String> {
    List<SpendingGoal> findByCustomerId(String customerId);
}
