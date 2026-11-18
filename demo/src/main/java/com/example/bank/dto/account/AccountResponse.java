package com.example.bank.dto.account;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.bank.enums.AccountType;

public record AccountResponse(
		Long id,
		String accountNumber,
		AccountType accountType,
		BigDecimal balance,
		Long customerId,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {
}
