package com.example.bank.repository;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.bank.entity.Transaction;
import com.example.bank.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

	Page<Transaction> findBySourceAccount_IdOrDestinationAccount_IdOrderByCreatedAtDesc(
			Long sourceAccountId, Long destinationAccountId, Pageable pageable);

	@Query("select coalesce(sum(transaction.amount), 0) from Transaction transaction "
			+ "where transaction.sourceAccount.id = :accountId and transaction.type = :type "
			+ "and transaction.createdAt >= :start and transaction.createdAt < :end")
	BigDecimal sumTransfersForAccountBetween(
			@Param("accountId") Long accountId,
			@Param("type") TransactionType type,
			@Param("start") Instant start,
			@Param("end") Instant end);
}
