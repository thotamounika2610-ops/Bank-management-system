package com.example.bank.controller;

import com.example.bank.dto.transaction.AmountRequest;
import com.example.bank.dto.transaction.TransactionResponse;
import com.example.bank.dto.transaction.TransferRequest;
import com.example.bank.service.TransactionService;
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
@RequestMapping("/api/accounts/{accountId}/transactions")
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
@Tag(name = "Transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

	private final TransactionService transactionService;

	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@PostMapping("/deposit")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Deposit funds into an account")
	public ResponseEntity<TransactionResponse> deposit(@PathVariable Long accountId,
													   @Valid @RequestBody AmountRequest request,
													   Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(transactionService.deposit(accountId, request, authentication));
	}

	@PostMapping("/withdraw")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Withdraw funds from an account")
	public ResponseEntity<TransactionResponse> withdraw(@PathVariable Long accountId,
														@Valid @RequestBody AmountRequest request,
														Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(transactionService.withdraw(accountId, request, authentication));
	}

	@PostMapping("/transfer")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Transfer funds to another account")
	public ResponseEntity<TransactionResponse> transfer(@PathVariable Long accountId,
														@Valid @RequestBody TransferRequest request,
														Authentication authentication) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(transactionService.transfer(accountId, request, authentication));
	}

	@GetMapping
	@Operation(summary = "Get paginated transaction history for an account")
	public Page<TransactionResponse> getTransactions(@PathVariable Long accountId, Pageable pageable,
													 Authentication authentication) {
		return transactionService.getTransactions(accountId, pageable, authentication);
	}
}
