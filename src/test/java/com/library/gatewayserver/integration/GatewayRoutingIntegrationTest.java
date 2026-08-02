package com.library.gatewayserver.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * gateway-server's actual job — routing requests to the right backend and
 * applying CORS — is defined entirely in config-server's gateway-server.yml,
 * externally to this codebase, and had never been verified by a test. Since
 * the test profile disables config-server import (spring.cloud.config.enabled
 * = false), the two routes are re-declared here as test-only properties,
 * mirroring config-server/src/main/resources/config/gateway-server.yml
 * exactly — keep these two in sync if the real routes ever change.
 *
 * book-service and administration-service are stood in for by WireMock,
 * resolved through Spring Cloud LoadBalancer's SimpleDiscoveryClient (same
 * technique as administration-service's AdminControllerIntegrationTest).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.gateway.server.webflux.routes[0].id=book-service",
        "spring.cloud.gateway.server.webflux.routes[0].uri=lb://book-service",
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/books,/api/books/**,/api/messages,/api/messages/**,/api/reviews,/api/reviews/**",
        "spring.cloud.gateway.server.webflux.routes[1].id=administration-service",
        "spring.cloud.gateway.server.webflux.routes[1].uri=lb://administration-service",
        "spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/admin,/api/admin/**,/api/histories,/api/histories/**,/api/payment,/api/payment/**",
        "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedOrigins[0]=http://localhost:3000",
        "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[0]=GET",
        "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[1]=POST",
        "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[2]=PUT",
        "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedMethods[3]=DELETE",
        "spring.cloud.gateway.server.webflux.globalcors.cors-configurations.[/**].allowedHeaders[0]=*",
})
@AutoConfigureWebTestClient
class GatewayRoutingIntegrationTest {

    static WireMockServer bookServiceWireMock;
    static WireMockServer administrationServiceWireMock;

    @BeforeAll
    static void startWireMockServers() {
        bookServiceWireMock = new WireMockServer(wireMockConfig().dynamicPort());
        bookServiceWireMock.start();
        administrationServiceWireMock = new WireMockServer(wireMockConfig().dynamicPort());
        administrationServiceWireMock.start();
    }

    @AfterAll
    static void stopWireMockServers() {
        bookServiceWireMock.stop();
        administrationServiceWireMock.stop();
    }

    @DynamicPropertySource
    static void discoveryInstances(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.book-service[0].uri",
                () -> "http://localhost:" + bookServiceWireMock.port());
        registry.add("spring.cloud.discovery.client.simple.instances.administration-service[0].uri",
                () -> "http://localhost:" + administrationServiceWireMock.port());
    }

    @org.springframework.beans.factory.annotation.Autowired
    WebTestClient webTestClient;

    @BeforeEach
    void resetStubs() {
        bookServiceWireMock.resetAll();
        administrationServiceWireMock.resetAll();
    }

    @Test
    void requestToApiBooks_routesToBookService() {
        bookServiceWireMock.stubFor(get(urlEqualTo("/api/books/1"))
                .willReturn(okJson("{\"id\":1,\"title\":\"Effective Java\"}")));

        webTestClient.get().uri("/api/books/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.title").isEqualTo("Effective Java");

        bookServiceWireMock.verify(getRequestedFor(urlEqualTo("/api/books/1")));
        administrationServiceWireMock.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void requestToApiAdmin_routesToAdministrationService() {
        administrationServiceWireMock.stubFor(get(urlEqualTo("/api/admin/secure/ping"))
                .willReturn(ok("pong")));

        webTestClient.get().uri("/api/admin/secure/ping")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("pong");

        administrationServiceWireMock.verify(getRequestedFor(urlEqualTo("/api/admin/secure/ping")));
        bookServiceWireMock.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void requestToApiPayment_routesToAdministrationService() {
        administrationServiceWireMock.stubFor(post(urlEqualTo("/api/payment/secure/payment-intent"))
                .willReturn(okJson("{\"client_secret\":\"pi_test_secret\"}")));

        webTestClient.post().uri("/api/payment/secure/payment-intent")
                .exchange()
                .expectStatus().isOk();

        administrationServiceWireMock.verify(postRequestedFor(urlEqualTo("/api/payment/secure/payment-intent")));
        bookServiceWireMock.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void unroutedPath_returns404() {
        webTestClient.get().uri("/api/nonexistent/path")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void preflightFromAllowedOrigin_getsCorsHeader() {
        bookServiceWireMock.stubFor(get(urlEqualTo("/api/books/1")).willReturn(ok()));

        webTestClient.options().uri("/api/books/1")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", HttpMethod.GET.name())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000");
    }
}
