package com.example.bank.entity;

import java.math.BigDecimal;

import com.example.bank.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bank_transactions", uniqueConstraints = {
		@UniqueConstraint(name = "uk_transactions_reference", columnNames = "reference")
})
@Getter
@Setter
@NoArgsConstructor
public class Transaction extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 36)
	private String reference;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransactionType type;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
	private BigDecimal balanceAfter;

	@Column(name = "destination_balance_after", precision = 19, scale = 2)
	private BigDecimal destinationBalanceAfter;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_account_id")
	private Account sourceAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "destination_account_id")
	private Account destinationAccount;

	@Column(length = 500)
	private String description;
}
