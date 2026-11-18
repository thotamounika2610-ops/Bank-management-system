package com.example.bank.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Locale;

import com.example.bank.dto.account.AccountCreateRequest;
import com.example.bank.dto.account.AccountResponse;
import com.example.bank.entity.Account;
import com.example.bank.entity.Customer;
import com.example.bank.exception.ResourceAccessDeniedException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

	private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("500.00");
	private static final long ACCOUNT_NUMBER_BOUND = 1_000_000_000_000L;

	private final AccountRepository accountRepository;
	private final CustomerRepository customerRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public AccountService(AccountRepository accountRepository, CustomerRepository customerRepository) {
		this.accountRepository = accountRepository;
		this.customerRepository = customerRepository;
	}

	@Transactional
	public AccountResponse createAccount(AccountCreateRequest request, Authentication authentication) {
		if (request.initialDeposit().compareTo(MINIMUM_BALANCE) < 0) {
			throw new IllegalArgumentException("Initial deposit must be at least 500.00");
		}
		Customer customer = customerRepository.findByUser_Email(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));

		Account account = new Account();
		account.setCustomer(customer);
		account.setAccountType(request.accountType());
		account.setBalance(request.initialDeposit());
		account.setAccountNumber(generateAccountNumber());
		return toResponse(accountRepository.save(account));
	}

	@Transactional(readOnly = true)
	public Page<AccountResponse> getAccounts(Pageable pageable, Authentication authentication) {
		if (isAdmin(authentication)) {
			return accountRepository.findAll(pageable).map(this::toResponse);
		}
		return accountRepository.findByCustomer_User_Email(authentication.getName(), pageable)
				.map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public AccountResponse getAccount(Long id, Authentication authentication) {
		Account account = accountRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Account not found"));
		checkOwnership(account, authentication);
		return toResponse(account);
	}

	private String generateAccountNumber() {
		String accountNumber;
		do {
			accountNumber = String.format(Locale.ROOT, "%012d",
					secureRandom.nextLong(ACCOUNT_NUMBER_BOUND));
		} while (accountRepository.existsByAccountNumber(accountNumber));
		return accountNumber;
	}

	private void checkOwnership(Account account, Authentication authentication) {
		if (!isAdmin(authentication)
				&& !account.getCustomer().getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
			throw new ResourceAccessDeniedException("You may only access your own accounts");
		}
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}

	private AccountResponse toResponse(Account account) {
		return new AccountResponse(account.getId(), account.getAccountNumber(), account.getAccountType(),
				account.getBalance(), account.getCustomer().getId(), account.isActive(),
				account.getCreatedAt(), account.getUpdatedAt());
	}
}
