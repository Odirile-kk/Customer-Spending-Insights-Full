package za.co.example.spending.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.example.spending.model.Customer;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByEmailIgnoreCase(String email);
}
