package com.library.gatewayserver;

import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ApiGatewayServerApplicationTests {

	@MockitoBean
	private Tracer tracer;

	@Test
	void contextLoads() {
	}

}
