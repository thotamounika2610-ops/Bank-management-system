package com.example.bank.dto.transaction;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
		@NotNull @Positive Long destinationAccountId,
		@NotNull @DecimalMin(value = "0.00", inclusive = false)
		@Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
