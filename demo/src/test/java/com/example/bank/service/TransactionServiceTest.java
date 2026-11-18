package com.example.bank.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.example.bank.dto.transaction.AmountRequest;
import com.example.bank.dto.transaction.TransferRequest;
import com.example.bank.entity.Account;
import com.example.bank.entity.Customer;
import com.example.bank.entity.Transaction;
import com.example.bank.entity.User;
import com.example.bank.enums.TransactionType;
import com.example.bank.exception.DailyLimitExceededException;
import com.example.bank.exception.InsufficientBalanceException;
import com.example.bank.exception.ResourceAccessDeniedException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private TransactionRepository transactionRepository;

	private TransactionService transactionService;

	@BeforeEach
	void setUp() {
		transactionService = new TransactionService(accountRepository, transactionRepository);
	}

	@Test
	void rejectsWithdrawalThatWouldDropBalanceBelowMinimum() {
		Account account = account(1L, "owner@example.test", "520.00");
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> transactionService.withdraw(1L, new AmountRequest(new BigDecimal("20.01")),
				customerAuthentication("owner@example.test")))
				.isInstanceOf(InsufficientBalanceException.class);

		assertThat(account.getBalance()).isEqualByComparingTo("520.00");
		verify(transactionRepository, never()).save(any(Transaction.class));
	}

	@Test
	void rejectsTransferThatExceedsDailyLimit() {
		Account source = account(1L, "owner@example.test", "100000.00");
		Account destination = account(2L, "owner@example.test", "600.00");
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
		when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));
		when(transactionRepository.sumTransfersForAccountBetween(
				org.mockito.ArgumentMatchers.eq(1L),
				org.mockito.ArgumentMatchers.eq(TransactionType.TRANSFER),
				any(Instant.class), any(Instant.class)))
				.thenReturn(new BigDecimal("49900.00"));

		assertThatThrownBy(() -> transactionService.transfer(1L,
				new TransferRequest(2L, new BigDecimal("200.00")),
				customerAuthentication("owner@example.test")))
				.isInstanceOf(DailyLimitExceededException.class);

		assertThat(source.getBalance()).isEqualByComparingTo("100000.00");
		assertThat(destination.getBalance()).isEqualByComparingTo("600.00");
		verify(transactionRepository, never()).save(any(Transaction.class));
	}

	@Test
	void successfulTransferUpdatesBothBalancesAndPersistsTransaction() {
		Account source = account(1L, "owner@example.test", "1000.00");
		Account destination = account(2L, "owner@example.test", "600.00");
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(source));
		when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(destination));
		when(transactionRepository.sumTransfersForAccountBetween(
				org.mockito.ArgumentMatchers.eq(1L),
				org.mockito.ArgumentMatchers.eq(TransactionType.TRANSFER),
				any(Instant.class), any(Instant.class)))
				.thenReturn(BigDecimal.ZERO);
		when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
			Transaction transaction = invocation.getArgument(0);
			transaction.setId(1L);
			return transaction;
		});

		var response = transactionService.transfer(1L,
				new TransferRequest(2L, new BigDecimal("200.00")),
				customerAuthentication("owner@example.test"));

		assertThat(source.getBalance()).isEqualByComparingTo("800.00");
		assertThat(destination.getBalance()).isEqualByComparingTo("800.00");
		assertThat(response.type()).isEqualTo(TransactionType.TRANSFER);
		assertThat(response.balanceAfter()).isEqualByComparingTo("800.00");
		assertThat(response.destinationBalanceAfter()).isEqualByComparingTo("800.00");
		verify(transactionRepository).save(any(Transaction.class));
	}

	@Test
	void deniesTransactionOnAnotherCustomersAccount() {
		Account account = account(1L, "owner@example.test", "1000.00");
		when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> transactionService.deposit(1L,
				new AmountRequest(new BigDecimal("50.00")),
				customerAuthentication("other@example.test")))
				.isInstanceOf(ResourceAccessDeniedException.class)
				.hasMessage("You may only access your own accounts");
		verify(transactionRepository, never()).save(any(Transaction.class));
	}

	private Account account(Long id, String ownerEmail, String balance) {
		User user = new User();
		user.setEmail(ownerEmail);
		Customer customer = new Customer();
		customer.setUser(user);
		Account account = new Account();
		account.setId(id);
		account.setCustomer(customer);
		account.setBalance(new BigDecimal(balance));
		return account;
	}

	private Authentication customerAuthentication(String email) {
		return UsernamePasswordAuthenticationToken.authenticated(email, null,
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
	}
}
