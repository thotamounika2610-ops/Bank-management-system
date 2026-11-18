package com.example.bank.dto.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.bank.enums.TransactionType;

public record TransactionResponse(
		Long id,
		String reference,
		TransactionType type,
		BigDecimal amount,
		BigDecimal balanceAfter,
		Long sourceAccountId,
		Long destinationAccountId,
		BigDecimal destinationBalanceAfter,
		Instant timestamp) {
}
