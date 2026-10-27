package za.co.example.spending.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.example.spending.model.SpendingTransaction;

import java.util.List;

public interface TransactionRepository extends JpaRepository<SpendingTransaction, String> {
    List<SpendingTransaction> findByCustomerId(String customerId);
}
