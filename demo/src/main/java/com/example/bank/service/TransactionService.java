package com.example.bank.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.example.bank.dto.transaction.AmountRequest;
import com.example.bank.dto.transaction.TransactionResponse;
import com.example.bank.dto.transaction.TransferRequest;
import com.example.bank.entity.Account;
import com.example.bank.entity.Transaction;
import com.example.bank.enums.TransactionType;
import com.example.bank.exception.AccountNotFoundException;
import com.example.bank.exception.DailyLimitExceededException;
import com.example.bank.exception.InsufficientBalanceException;
import com.example.bank.exception.ResourceAccessDeniedException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

	private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("500.00");
	private static final BigDecimal DAILY_TRANSFER_LIMIT = new BigDecimal("50000.00");

	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;

	public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
	}

	@Transactional
	public TransactionResponse deposit(Long accountId, AmountRequest request, Authentication authentication) {
		Account account = lockAccount(accountId);
		checkOwnership(account, authentication);
		account.setBalance(account.getBalance().add(request.amount()));
		return saveTransaction(TransactionType.DEPOSIT, request.amount(), account.getBalance(), account, null);
	}

	@Transactional
	public TransactionResponse withdraw(Long accountId, AmountRequest request, Authentication authentication) {
		Account account = lockAccount(accountId);
		checkOwnership(account, authentication);
		BigDecimal newBalance = account.getBalance().subtract(request.amount());
		ensureMinimumBalance(newBalance);
		account.setBalance(newBalance);
		return saveTransaction(TransactionType.WITHDRAWAL, request.amount(), newBalance, account, null);
	}

	@Transactional
	public TransactionResponse transfer(Long sourceAccountId, TransferRequest request,
			Authentication authentication) {
		Long destinationAccountId = request.destinationAccountId();
		if (sourceAccountId.equals(destinationAccountId)) {
			throw new IllegalArgumentException("Source and destination accounts must be different");
		}

		// All transfer paths lock accounts by ID so opposite-direction transfers
		// acquire locks consistently.
		Long firstId = Math.min(sourceAccountId, destinationAccountId);
		Long secondId = Math.max(sourceAccountId, destinationAccountId);
		Account first = lockAccount(firstId);
		Account second = lockAccount(secondId);
		Account source = sourceAccountId.equals(firstId) ? first : second;
		Account destination = destinationAccountId.equals(firstId) ? first : second;

		checkOwnership(source, authentication);
		checkOwnership(destination, authentication);

		BigDecimal sourceBalance = source.getBalance().subtract(request.amount());
		ensureMinimumBalance(sourceBalance);
		ensureDailyLimit(source.getId(), request.amount());

		source.setBalance(sourceBalance);
		BigDecimal destinationBalance = destination.getBalance().add(request.amount());
		destination.setBalance(destinationBalance);
		return saveTransaction(TransactionType.TRANSFER, request.amount(), sourceBalance,
				destinationBalance, source, destination);
	}

	@Transactional(readOnly = true)
	public Page<TransactionResponse> getTransactions(Long accountId, Pageable pageable,
			Authentication authentication) {
		Account account = accountRepository.findById(accountId)
				.orElseThrow(() -> new AccountNotFoundException("Account not found"));
		checkOwnership(account, authentication);
		return transactionRepository
				.findBySourceAccount_IdOrDestinationAccount_IdOrderByCreatedAtDesc(accountId, accountId, pageable)
				.map(transaction -> toResponse(transaction, accountId));
	}

	private Account lockAccount(Long id) {
		return accountRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new AccountNotFoundException("Account not found"));
	}

	private void ensureMinimumBalance(BigDecimal balance) {
		if (balance.compareTo(MINIMUM_BALANCE) < 0) {
			throw new InsufficientBalanceException("Account balance cannot fall below 500.00");
		}
	}

	private void ensureDailyLimit(Long accountId, BigDecimal amount) {
		ZoneOffset zone = ZoneOffset.UTC;
		LocalDate today = LocalDate.now(zone);
		Instant start = today.atStartOfDay(zone).toInstant();
		Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();
		BigDecimal transferredToday = transactionRepository.sumTransfersForAccountBetween(
				accountId, TransactionType.TRANSFER, start, end);
		if (transferredToday.add(amount).compareTo(DAILY_TRANSFER_LIMIT) > 0) {
			throw new DailyLimitExceededException("Daily transfer limit of 50000.00 exceeded");
		}
	}

	private TransactionResponse saveTransaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
			Account source, Account destination) {
		BigDecimal destinationBalanceAfter = destination == null ? null : destination.getBalance();
		return saveTransaction(type, amount, balanceAfter, destinationBalanceAfter, source, destination);
	}

	private TransactionResponse saveTransaction(TransactionType type, BigDecimal amount, BigDecimal balanceAfter,
			BigDecimal destinationBalanceAfter, Account source,
			Account destination) {
		Transaction transaction = new Transaction();
		transaction.setReference(UUID.randomUUID().toString());
		transaction.setType(type);
		transaction.setAmount(amount);
		transaction.setBalanceAfter(balanceAfter);
		transaction.setDestinationBalanceAfter(destinationBalanceAfter);
		transaction.setSourceAccount(source);
		transaction.setDestinationAccount(destination);
		return toResponse(transactionRepository.save(transaction));
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

	private TransactionResponse toResponse(Transaction transaction) {
		return toResponse(transaction, null);
	}

	private TransactionResponse toResponse(Transaction transaction, Long accountId) {
		BigDecimal balanceAfter = transaction.getBalanceAfter();
		if (accountId != null && transaction.getDestinationAccount() != null
				&& transaction.getDestinationAccount().getId().equals(accountId)) {
			balanceAfter = transaction.getDestinationBalanceAfter();
		}
		return new TransactionResponse(transaction.getId(), transaction.getReference(), transaction.getType(),
				transaction.getAmount(), balanceAfter,
				transaction.getSourceAccount() == null ? null : transaction.getSourceAccount().getId(),
				transaction.getDestinationAccount() == null ? null : transaction.getDestinationAccount().getId(),
				transaction.getDestinationBalanceAfter(),
				transaction.getCreatedAt());
	}
}
