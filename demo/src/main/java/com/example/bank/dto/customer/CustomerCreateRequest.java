package com.example.bank.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CustomerCreateRequest(
		@NotNull @Positive Long userId,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Pattern(regexp = "^$|^[0-9+()\\- ]{7,30}$") String phoneNumber) {
}
