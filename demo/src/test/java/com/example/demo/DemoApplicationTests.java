package com.example.demo;

import com.example.bank.BankApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = BankApplication.class,
		properties = {
				"spring.datasource.url=jdbc:h2:mem:bankdb;DB_CLOSE_DELAY=-1",
				"spring.datasource.username=sa",
				"spring.datasource.password=",
				"spring.datasource.driver-class-name=org.h2.Driver",
				"spring.jpa.hibernate.ddl-auto=create-drop",
				"JWT_SECRET=VGhpc0lzQVRlc3RTZWNyZXRLZXlGb3JKV1QzMjU2Qml0cw=="
		}
)
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
