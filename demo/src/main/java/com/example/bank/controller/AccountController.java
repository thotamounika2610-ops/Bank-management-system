package com.example.bank.controller;

import com.example.bank.dto.account.AccountCreateRequest;
import com.example.bank.dto.account.AccountResponse;
import com.example.bank.service.AccountService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
@Tag(name = "Accounts")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Create an account for the authenticated customer")
	public ResponseEntity<AccountResponse> createAccount(
			@Valid @RequestBody AccountCreateRequest request, Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(accountService.createAccount(request, authentication));
	}

	@GetMapping
	@Operation(summary = "List accounts visible to the authenticated user")
	public Page<AccountResponse> getAccounts(Pageable pageable, Authentication authentication) {
		return accountService.getAccounts(pageable, authentication);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get an account by ID")
	public AccountResponse getAccount(@PathVariable Long id, Authentication authentication) {
		return accountService.getAccount(id, authentication);
	}
}
