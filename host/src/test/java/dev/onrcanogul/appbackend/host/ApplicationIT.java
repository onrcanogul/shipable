package dev.onrcanogul.appbackend.host;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The one test that starts everything: real Postgres, all migrations, the whole filter
 * chain.
 *
 * <p>The module tests prove each piece wires up. This proves the pieces fit — in
 * particular that every module's Flyway migrations apply cleanly to one database in one
 * order, and that Hibernate's {@code ddl-auto: validate} agrees with the resulting schema.
 * That check is the reason to keep it: an entity field with no matching column is the kind
 * of mistake that otherwise surfaces on deploy.
 *
 * <p>Skipped when Docker is not running, rather than failing. A developer without Docker
 * should still get a green {@code mvn verify}; CI has Docker and runs it for real.
 */
@Testcontainers
@EnabledIf("dockerAvailable")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Secrets the application refuses to start without. */
    @DynamicPropertySource
    static void testSecrets(DynamicPropertyRegistry registry) {
        registry.add("app.identity.jwt.secret", () -> "an-integration-test-signing-secret-32b+");
        registry.add("app.billing.revenue-cat.api-key", () -> "test-api-key");
        registry.add("app.billing.revenue-cat.webhook-secret", () -> "test-webhook-secret");
    }

    @Test
    @DisplayName("the application starts, migrations apply, and the schema validates")
    void applicationStarts() {
        ResponseEntity<String> response = get("/api/v1/health");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("ok");
    }

    @Test
    @DisplayName("config is reachable before sign-in")
    void configIsPublic() {
        assertThat(get("/api/v1/config").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("an endpoint that needs a user answers 401 without one, in the standard error shape")
    void unauthenticatedRequestsAreRejected() {
        ResponseEntity<String> response = get("/api/v1/billing/entitlements");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("\"code\":\"unauthorized\"");
    }

    @Test
    @DisplayName("the RevenueCat webhook rejects a caller without the shared secret")
    void webhookRejectsStrangers() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/billing/webhooks/revenuecat"), "{\"event\":{}}", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("every response carries a request id")
    void responsesAreTraceable() {
        assertThat(get("/api/v1/health").getHeaders().getFirst("X-Request-Id")).isNotBlank();
    }

    @Test
    @DisplayName("the OpenAPI document is generated")
    void openApiIsServed() {
        ResponseEntity<String> response = get("/v3/api-docs");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("/api/v1/auth/social");
    }

    private ResponseEntity<String> get(String path) {
        return restTemplate.getForEntity(url(path), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
