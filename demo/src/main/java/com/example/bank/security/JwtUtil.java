package com.example.bank.security;

import java.util.Date;

import javax.crypto.SecretKey;

import com.example.bank.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

	private static final String ROLE_CLAIM = "role";

	private final SecretKey signingKey;
	private final long expirationMs;

	public JwtUtil(
			@Value("${JWT_SECRET}") String base64Secret,
			@Value("${jwt.expiration-ms:3600000}") long expirationMs) {
		byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
		this.signingKey = Keys.hmacShaKeyFor(keyBytes);
		this.expirationMs = expirationMs;
		if (expirationMs <= 0) {
			throw new IllegalArgumentException("JWT expiration must be positive");
		}
	}

	public String generateToken(UserDetails userDetails) {
		Date issuedAt = new Date();
		Date expiresAt = new Date(issuedAt.getTime() + expirationMs);
		return Jwts.builder()
				.subject(userDetails.getUsername())
				.claim(ROLE_CLAIM, extractRole(userDetails).name())
				.issuedAt(issuedAt)
				.expiration(expiresAt)
				.signWith(signingKey)
				.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public Role extractRole(String token) {
		String role = parseClaims(token).get(ROLE_CLAIM, String.class);
		if (role == null || role.isBlank()) {
			throw new IllegalArgumentException("JWT is missing the role claim");
		}
		return Role.valueOf(role);
	}

	public boolean validateToken(String token, UserDetails userDetails) {
		Claims claims = parseClaims(token);
		return claims.getSubject().equals(userDetails.getUsername())
				&& claims.getExpiration().after(new Date())
				&& extractRole(userDetails).name().equals(claims.get(ROLE_CLAIM, String.class));
	}

	public long getExpirationMs() {
		return expirationMs;
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private Role extractRole(UserDetails userDetails) {
		String role = userDetails.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(authority -> authority.startsWith("ROLE_"))
				.map(authority -> authority.substring("ROLE_".length()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("User must have a role authority"));
		return Role.valueOf(role);
	}
}
