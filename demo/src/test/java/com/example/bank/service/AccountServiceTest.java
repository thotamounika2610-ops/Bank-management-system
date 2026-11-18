package com.example.bank.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.example.bank.dto.account.AccountCreateRequest;
import com.example.bank.entity.Account;
import com.example.bank.entity.Customer;
import com.example.bank.entity.User;
import com.example.bank.enums.AccountType;
import com.example.bank.exception.ResourceAccessDeniedException;
import com.example.bank.repository.AccountRepository;
import com.example.bank.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private CustomerRepository customerRepository;

	private AccountService accountService;

	@BeforeEach
	void setUp() {
		accountService = new AccountService(accountRepository, customerRepository);
	}

	@Test
	void rejectsInitialDepositBelowMinimumBalance() {
		AccountCreateRequest request = new AccountCreateRequest(AccountType.SAVINGS, new BigDecimal("499.99"));

		assertThatThrownBy(() -> accountService.createAccount(request, customerAuthentication("owner@example.test")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Initial deposit must be at least 500.00");
		verify(customerRepository, never()).findByUser_Email(org.mockito.ArgumentMatchers.anyString());
		verify(accountRepository, never()).save(org.mockito.ArgumentMatchers.any(Account.class));
	}

	@Test
	void deniesCustomerAccessToAnotherCustomersAccount() {
		User owner = new User();
		owner.setEmail("owner@example.test");
		Customer customer = new Customer();
		customer.setUser(owner);
		Account account = new Account();
		account.setCustomer(customer);
		when(accountRepository.findById(17L)).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> accountService.getAccount(17L,
				customerAuthentication("other@example.test")))
				.isInstanceOf(ResourceAccessDeniedException.class)
				.hasMessage("You may only access your own accounts");
	}

	private Authentication customerAuthentication(String email) {
		return UsernamePasswordAuthenticationToken.authenticated(email, null,
				List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
	}
}
