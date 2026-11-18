package com.example.bank.dto.account;

import java.math.BigDecimal;

import com.example.bank.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record AccountCreateRequest(
		@NotNull AccountType accountType,
		@NotNull @DecimalMin(value = "500.00")
		@Digits(integer = 17, fraction = 2) BigDecimal initialDeposit) {
}
