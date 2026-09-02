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
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The one test that starts everything: real Postgres, every migration, the whole filter
 * chain.
 *
 * <p>The module tests prove each piece wires up. This proves the pieces fit — and it earns
 * its keep. It is what caught a bean name colliding with Spring's own
 * {@code requestContextFilter}, and Flyway refusing to start because every module owns a
 * migration numbered V1. Neither is visible from a module test, and both would have
 * surfaced on first deploy.
 *
 * <p>Skipped when Docker is not running, rather than failing: a developer without Docker
 * should still get a green {@code mvn verify}. That is also the trap — the two bugs above
 * sat unnoticed while it was skipping. Run it before you ship.
 */
@Testcontainers
@EnabledIf("dockerAvailable")
@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationIT {

    private static final String ADMIN_KEY = "an-integration-test-admin-key-of-32-plus";

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
        registry.add("app.admin.api-key", () -> ADMIN_KEY);
    }

    @Test
    @DisplayName("the application starts, every module's migrations apply, and the schema validates")
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/billing/webhooks/revenuecat"),
                new HttpEntity<>("{\"event\":{}}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a wrong content type is 415, not a 500 that looks like the server broke")
    void wrongContentTypeIsAClientError() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/v1/billing/webhooks/revenuecat"),
                new HttpEntity<>("not json", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("the admin API refuses a missing or wrong key")
    void adminRejectsWithoutTheKey() {
        assertThat(get("/api/admin/v1/settings").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(getWithAdminKey("/api/admin/v1/settings", "wrong").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("the admin API lists the settings an operator can change")
    void adminListsSettings() {
        ResponseEntity<String> response = getWithAdminKey("/api/admin/v1/settings", ADMIN_KEY);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Contributed by core and by the cache module, proving catalogs from several
        // modules are collected rather than just one.
        assertThat(response.getBody())
                .contains("core.rate-limit.permits")
                .contains("cache.bypass");
    }

    @Test
    @DisplayName("anonymous sign-in issues a session, and the same device gets the same user")
    void anonymousSignInIsFindOrCreate() {
        String deviceId = "it-device-" + java.util.UUID.randomUUID();

        Map<String, Object> first = signInAnonymously(deviceId);
        Map<String, Object> second = signInAnonymously(deviceId);

        assertThat(first.get("accessToken")).asString().isNotBlank();
        assertThat(first.get("anonymous")).isEqualTo(true);
        // Find-or-create, not create: a retry must not strand the first account's history.
        assertThat(second.get("userId")).isEqualTo(first.get("userId"));
    }

    @Test
    @DisplayName("the issued token authenticates a protected endpoint")
    void issuedTokenWorks() {
        Map<String, Object> session = signInAnonymously("it-device-" + java.util.UUID.randomUUID());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) session.get("accessToken"));
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/billing/entitlements"), HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"paying\":false");
    }

    @Test
    @DisplayName("refresh rotates the token, and the old one stops working")
    void refreshRotates() {
        Map<String, Object> session = signInAnonymously("it-device-" + java.util.UUID.randomUUID());
        String original = (String) session.get("refreshToken");

        ResponseEntity<Map<String, Object>> refreshed = postJson("/api/v1/auth/refresh",
                Map.of("refreshToken", original));
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody()).isNotNull();
        assertThat(refreshed.getBody().get("refreshToken")).isNotEqualTo(original);

        // Replaying the rotated token must not extend the session.
        assertThat(postJson("/api/v1/auth/refresh", Map.of("refreshToken", original)).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("an anonymous session cannot delete the account")
    void anonymousCannotDeleteTheAccount() {
        Map<String, Object> session = signInAnonymously("it-device-" + java.util.UUID.randomUUID());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth((String) session.get("accessToken"));
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/account/deletion"), HttpMethod.POST, new HttpEntity<>(headers), String.class);

        // requireRegistered: otherwise anyone holding the phone could delete the account.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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

    private Map<String, Object> signInAnonymously(String deviceId) {
        ResponseEntity<Map<String, Object>> response =
                postJson("/api/v1/auth/anonymous", Map.of("deviceId", deviceId));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private ResponseEntity<Map<String, Object>> postJson(String path, Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(
                url(path), HttpMethod.POST, new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<Map<String, Object>>() { });
    }

    private ResponseEntity<String> get(String path) {
        return restTemplate.getForEntity(url(path), String.class);
    }

    private ResponseEntity<String> getWithAdminKey(String path, String key) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Admin-Key", key);
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
