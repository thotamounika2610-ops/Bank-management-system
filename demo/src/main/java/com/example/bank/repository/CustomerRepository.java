package com.example.bank.repository;

import java.util.Optional;

import com.example.bank.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	Optional<Customer> findByUserId(Long userId);

	Optional<Customer> findByUser_Email(String email);
}
