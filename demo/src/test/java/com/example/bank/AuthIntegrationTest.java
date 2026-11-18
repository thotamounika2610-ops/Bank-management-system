package com.example.bank;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.UUID;

import com.example.bank.dto.auth.AuthResponse;
import com.example.bank.dto.auth.LoginRequest;
import com.example.bank.dto.auth.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest(
		classes = BankApplication.class,
		properties = {
				"spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.jpa.hibernate.ddl-auto=create-drop",
				"JWT_SECRET=VGhpc0lzQVRlc3RTZWNyZXRLZXlGb3JKV1QzMjU2Qml0cw==",
				"jwt.expiration-ms=3600000"
		}
)
@AutoConfigureMockMvc
class AuthIntegrationTest {

	private static final String PASSWORD = "secure-test-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void loginReturnsAccessTokenForValidCredentials() throws Exception {
		String email = UUID.randomUUID() + "@example.test";
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterRequest(email, PASSWORD, "Test", "Customer", null))))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"));
	}

	@Test
	void registersAndAuthenticatesCustomerWithoutAllowingRoleEscalation() throws Exception {
		String email = UUID.randomUUID() + "@example.test";
		String registrationJson = objectMapper.writeValueAsString(new RegisterRequest(
				email, PASSWORD, "Test", "Customer", null));
		registrationJson = registrationJson.substring(0, registrationJson.length() - 1)
				+ ",\"role\":\"ADMIN\"}";

		MvcResult registration = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registrationJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("CUSTOMER"))
				.andReturn();
		AuthResponse registered = objectMapper.readValue(
				registration.getResponse().getContentAsString(), AuthResponse.class);
		assertThat(registered.accessToken()).isNotBlank();

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new LoginRequest(email.toUpperCase(Locale.ROOT), PASSWORD))))
				.andExpect(status().isOk())
				.andReturn();
		AuthResponse authenticated = objectMapper.readValue(
				login.getResponse().getContentAsString(), AuthResponse.class);

		mockMvc.perform(get("/api/customer/probe")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + authenticated.accessToken()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/admin/probe")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + authenticated.accessToken()))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new LoginRequest(email, "wrong-password"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void notFoundAccountReturnsStandardApiErrorFormat() throws Exception {
		String email = UUID.randomUUID() + "@example.test";
		MvcResult registration = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new RegisterRequest(email, PASSWORD, "Test", "Customer", null))))
				.andExpect(status().isCreated())
				.andReturn();
		AuthResponse registered = objectMapper.readValue(
				registration.getResponse().getContentAsString(), AuthResponse.class);

		MvcResult result = mockMvc.perform(get("/api/accounts/999999/transactions?page=0&size=20")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + registered.accessToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("Account not found"))
				.andReturn();
		JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
		assertThat(error.path("timestamp").asText()).isNotBlank();
		assertThat(error.path("validationErrors").isObject()).isTrue();
		assertThat(error.path("validationErrors").size()).isZero();
	}

	@Test
	void openApiDocumentsArePublicAndDeclareJwtBearerAuthentication() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
	}
}
