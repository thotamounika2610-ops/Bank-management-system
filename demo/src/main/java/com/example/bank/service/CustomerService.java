package com.example.bank.service;

import com.example.bank.dto.customer.CustomerCreateRequest;
import com.example.bank.dto.customer.CustomerResponse;
import com.example.bank.dto.customer.CustomerUpdateRequest;
import com.example.bank.entity.Customer;
import com.example.bank.entity.User;
import com.example.bank.exception.ResourceAccessDeniedException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.repository.CustomerRepository;
import com.example.bank.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final UserRepository userRepository;

	public CustomerService(CustomerRepository customerRepository, UserRepository userRepository) {
		this.customerRepository = customerRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public Page<CustomerResponse> getCustomers(Pageable pageable) {
		return customerRepository.findAll(pageable).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public CustomerResponse getCustomer(Long id, Authentication authentication) {
		Customer customer = findCustomer(id);
		checkOwnership(customer, authentication);
		return toResponse(customer);
	}

	@Transactional(readOnly = true)
	public CustomerResponse getMyProfile(Authentication authentication) {
		return customerRepository.findByUser_Email(authentication.getName())
				.map(this::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
	}

	@Transactional
	public CustomerResponse createCustomer(CustomerCreateRequest request) {
		User user = userRepository.findById(request.userId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		if (customerRepository.findByUserId(user.getId()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"A customer profile already exists for this user");
		}

		Customer customer = new Customer();
		customer.setUser(user);
		applyUpdate(customer, request.firstName(), request.lastName(), request.phoneNumber());
		return toResponse(customerRepository.save(customer));
	}

	@Transactional
	public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request,
										  Authentication authentication) {
		Customer customer = findCustomer(id);
		checkOwnership(customer, authentication);
		applyUpdate(customer, request.firstName(), request.lastName(), request.phoneNumber());
		return toResponse(customerRepository.save(customer));
	}

	@Transactional
	public CustomerResponse updateMyProfile(CustomerUpdateRequest request, Authentication authentication) {
		Customer customer = customerRepository.findByUser_Email(authentication.getName())
				.orElseThrow(() -> new ResourceNotFoundException("Customer profile not found"));
		applyUpdate(customer, request.firstName(), request.lastName(), request.phoneNumber());
		return toResponse(customerRepository.save(customer));
	}

	@Transactional
	public void deleteCustomer(Long id) {
		customerRepository.delete(findCustomer(id));
	}

	private Customer findCustomer(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
	}

	private void checkOwnership(Customer customer, Authentication authentication) {
		if (!isAdmin(authentication)
				&& !customer.getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
			throw new ResourceAccessDeniedException("You may only access your own customer profile");
		}
	}

	private boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
	}

	private void applyUpdate(Customer customer, String firstName, String lastName, String phoneNumber) {
		customer.setFirstName(firstName.trim());
		customer.setLastName(lastName.trim());
		customer.setPhoneNumber(phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber.trim());
	}

	private CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getUser().getId(),
				customer.getUser().getEmail(), customer.getFirstName(), customer.getLastName(),
				customer.getPhoneNumber(), customer.getCreatedAt(), customer.getUpdatedAt());
	}
}
