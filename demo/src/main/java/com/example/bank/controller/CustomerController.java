package com.example.bank.controller;

import com.example.bank.dto.customer.CustomerCreateRequest;
import com.example.bank.dto.customer.CustomerResponse;
import com.example.bank.dto.customer.CustomerUpdateRequest;
import com.example.bank.service.CustomerService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

	private final CustomerService customerService;

	public CustomerController(CustomerService customerService) {
		this.customerService = customerService;
	}

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "List customers")
	public Page<CustomerResponse> getCustomers(Pageable pageable) {
		return customerService.getCustomers(pageable);
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Get the authenticated customer's profile")
	public CustomerResponse getMyProfile(Authentication authentication) {
		return customerService.getMyProfile(authentication);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
	@Operation(summary = "Get a customer by ID")
	public CustomerResponse getCustomer(@PathVariable Long id, Authentication authentication) {
		return customerService.getCustomer(id, authentication);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Create a customer")
	public ResponseEntity<CustomerResponse> createCustomer(
			@Valid @RequestBody CustomerCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
	}

	@PutMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	@Operation(summary = "Update the authenticated customer's profile")
	public CustomerResponse updateMyProfile(@Valid @RequestBody CustomerUpdateRequest request,
											Authentication authentication) {
		return customerService.updateMyProfile(request, authentication);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
	@Operation(summary = "Update a customer by ID")
	public CustomerResponse updateCustomer(@PathVariable Long id,
										   @Valid @RequestBody CustomerUpdateRequest request,
										   Authentication authentication) {
		return customerService.updateCustomer(id, request, authentication);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Delete a customer by ID")
	public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
		customerService.deleteCustomer(id);
		return ResponseEntity.noContent().build();
	}
}
