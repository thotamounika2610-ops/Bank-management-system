package com.example.bank.entity;

import java.math.BigDecimal;

import com.example.bank.enums.AccountType;
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
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts", uniqueConstraints = {
		@UniqueConstraint(name = "uk_accounts_account_number", columnNames = "account_number")
})
@Getter
@Setter
@NoArgsConstructor
public class Account extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_number", nullable = false, length = 12)
	@Pattern(regexp = "\\d{12}", message = "Account number must contain exactly 12 digits")
	private String accountNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AccountType accountType;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal balance = BigDecimal.ZERO;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(nullable = false)
	private boolean active = true;
}
