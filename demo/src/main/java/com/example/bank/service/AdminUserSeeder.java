package com.example.bank.service;

import java.util.Locale;

import com.example.bank.entity.User;
import com.example.bank.enums.Role;
import com.example.bank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminUserSeeder implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(AdminUserSeeder.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String adminUsername;
	private final String adminPassword;

	public AdminUserSeeder(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${ADMIN_USERNAME:}") String adminUsername,
			@Value("${ADMIN_PASSWORD:}") String adminPassword) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminUsername = adminUsername;
		this.adminPassword = adminPassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (adminUsername.isBlank() && adminPassword.isBlank()) {
			logger.info("ADMIN_USERNAME and ADMIN_PASSWORD are not configured; skipping admin seeding");
			return;
		}
		if (adminUsername.isBlank() || adminPassword.isBlank()) {
			throw new IllegalStateException(
					"Both ADMIN_USERNAME and ADMIN_PASSWORD must be configured to seed the admin");
		}

		String email = adminUsername.trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(email)) {
			logger.info("Admin user already exists; skipping admin seeding");
			return;
		}

		User admin = new User();
		admin.setEmail(email);
		admin.setPasswordHash(passwordEncoder.encode(adminPassword));
		admin.setRole(Role.ADMIN);
		admin.setEnabled(true);
		userRepository.save(admin);
		logger.info("Seeded initial ADMIN account");
	}
}
