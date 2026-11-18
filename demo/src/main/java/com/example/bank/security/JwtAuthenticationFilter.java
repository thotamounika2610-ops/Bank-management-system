package com.example.bank.security;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserDetailsService userDetailsService;
	private final ObjectMapper objectMapper;

	public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService,
								   ObjectMapper objectMapper) {
		this.jwtUtil = jwtUtil;
		this.userDetailsService = userDetailsService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorization.substring(7).trim();
		if (token.isEmpty()) {
			writeUnauthorized(response);
			return;
		}

		try {
			String username = jwtUtil.extractUsername(token);
			if (SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);
				if (userDetails.isEnabled() && jwtUtil.validateToken(token, userDetails)) {
					UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(
									userDetails, null, userDetails.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
		} catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
			writeUnauthorized(response);
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void writeUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		objectMapper.writeValue(response.getOutputStream(), Map.of(
				"timestamp", java.time.Instant.now(),
				"status", HttpServletResponse.SC_UNAUTHORIZED,
				"error", "Unauthorized",
				"message", "Invalid or expired bearer token",
				"validationErrors", Map.of()));
	}
}
