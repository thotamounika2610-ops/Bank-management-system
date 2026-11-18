package com.example.bank.service;

import com.example.bank.dto.auth.AuthResponse;
import com.example.bank.dto.auth.LoginRequest;
import com.example.bank.dto.auth.RegisterRequest;
import com.example.bank.entity.Customer;
import com.example.bank.entity.User;
import com.example.bank.enums.Role;
import com.example.bank.repository.CustomerRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.security.JwtUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final CustomerRepository customerRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtUtil jwtUtil;

	public AuthService(UserRepository userRepository, CustomerRepository customerRepository,
					   PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
					   JwtUtil jwtUtil) {
		this.userRepository = userRepository;
		this.customerRepository = customerRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtUtil = jwtUtil;
	}

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
		}

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(Role.CUSTOMER);
		user.setEnabled(true);
		try {
			user = userRepository.saveAndFlush(user);

			Customer customer = new Customer();
			customer.setUser(user);
			customer.setFirstName(request.firstName().trim());
			customer.setLastName(request.lastName().trim());
			customer.setPhoneNumber(request.phoneNumber() == null
					|| request.phoneNumber().isBlank() ? null : request.phoneNumber().trim());
			customerRepository.save(customer);
		} catch (DataIntegrityViolationException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered", exception);
		}

		UserDetails userDetails = org.springframework.security.core.userdetails.User
				.withUsername(user.getEmail())
				.password(user.getPasswordHash())
				.authorities("ROLE_" + user.getRole().name())
				.build();
		return createAuthResponse(user, userDetails);
	}

	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.password()));
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
						"Invalid email or password"));
		return createAuthResponse(user, userDetails);
	}

	private AuthResponse createAuthResponse(User user, UserDetails userDetails) {
		return new AuthResponse(jwtUtil.generateToken(userDetails), "Bearer",
				jwtUtil.getExpirationMs() / 1000, user.getRole());
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(java.util.Locale.ROOT);
	}
}
