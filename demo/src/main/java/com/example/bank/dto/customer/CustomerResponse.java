package com.example.bank.dto.customer;

import java.time.Instant;

public record CustomerResponse(
		Long id,
		Long userId,
		String email,
		String firstName,
		String lastName,
		String phoneNumber,
		Instant createdAt,
		Instant updatedAt) {
}
