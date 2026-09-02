package dev.onrcanogul.appbackend.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.core.api.port.IdempotencyStore;
import dev.onrcanogul.appbackend.core.api.port.RateLimitPolicy;
import dev.onrcanogul.appbackend.core.api.port.RateLimiter;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.web.ProblemResponseWriter;
import dev.onrcanogul.appbackend.core.api.support.InMemoryIdempotencyStore;
import dev.onrcanogul.appbackend.core.api.support.InMemoryRateLimiter;
import dev.onrcanogul.appbackend.core.api.support.PropertiesRuntimeSettings;
import dev.onrcanogul.appbackend.core.internal.web.GlobalExceptionHandler;
import dev.onrcanogul.appbackend.core.internal.web.IdempotencyFilter;
import dev.onrcanogul.appbackend.core.internal.web.RateLimitFilter;
import dev.onrcanogul.appbackend.core.internal.web.RequestContextFilter;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Everything core hands to the outside world.
 *
 * <p>The host picks this class up with {@code @Import}. The {@code internal} package is
 * never opened to component scanning, so this file is the complete list of what core
 * contributes — no hidden beans appearing from a package scan.
 *
 * <p>Filter order is deliberate and worth reading top to bottom:
 * <ol>
 *   <li>{@link RequestContextFilter} — everything after it can log a request id.</li>
 *   <li>{@link RateLimitFilter} — reject floods before spending anything on them.</li>
 *   <li>{@link IdempotencyFilter} — catch retries before they reach a controller.</li>
 * </ol>
 * Authentication slots in after these; see {@code identity}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CoreProperties.class)
public class CoreModuleConfiguration {

    /**
     * A single {@link Clock} bean so anything time-dependent can be tested by swapping it
     * for a fixed one. Calling {@code Instant.now()} directly makes a class untestable.
     */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Overrides for settings that can change without a redeploy.
     *
     * <p>The fallback resolves nothing, so every caller gets its boot default. When
     * {@code appconfig} is present it publishes a database-backed implementation and
     * this one steps aside.
     */
    @Bean
    @ConditionalOnMissingBean(RuntimeSettings.class)
    public RuntimeSettings runtimeSettings() {
        return new PropertiesRuntimeSettings();
    }

    /**
     * Always registered, even when Redis is enabled.
     *
     * <p>With Redis on, {@code platform/cache} publishes {@code @Primary} versions that
     * take over, and wraps these as the fallback for when Redis is unreachable. Keeping
     * them as beans also keeps them covered by the eviction job below - a fallback that
     * nobody cleans up is a memory leak waiting for an outage.
     */
    @Bean
    public InMemoryRateLimiter inMemoryRateLimiter(Clock clock) {
        return new InMemoryRateLimiter(clock);
    }

    @Bean
    public InMemoryIdempotencyStore inMemoryIdempotencyStore(Clock clock) {
        return new InMemoryIdempotencyStore(clock);
    }

    /**
     * Shared by every filter that answers an error itself, so filters and the exception
     * handler cannot drift into two different error formats.
     */
    @Bean
    public ProblemResponseWriter problemResponseWriter(ObjectMapper objectMapper) {
        return new ProblemResponseWriter(objectMapper);
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
        return register(new RequestContextFilter(), Ordered.HIGHEST_PRECEDENCE + 10);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RateLimiter rateLimiter,
            CoreProperties properties,
            RuntimeSettings settings,
            ProblemResponseWriter problemWriter) {
        RateLimitPolicy bootDefault = new RateLimitPolicy(
                properties.rateLimit().permits(), properties.rateLimit().window());
        return register(new RateLimitFilter(rateLimiter, bootDefault, settings, problemWriter),
                Ordered.HIGHEST_PRECEDENCE + 20);
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilter(
            IdempotencyStore store, ProblemResponseWriter problemWriter) {
        return register(new IdempotencyFilter(store, problemWriter), Ordered.HIGHEST_PRECEDENCE + 30);
    }

    /**
     * Housekeeping for the in-memory stores. Without it both maps grow one entry per
     * distinct key forever — a slow leak an attacker gets to choose the keys for.
     *
     */
    @Bean
    public InMemoryStoreEviction inMemoryStoreEviction(
            InMemoryRateLimiter rateLimiter, InMemoryIdempotencyStore idempotencyStore) {
        return new InMemoryStoreEviction(rateLimiter, idempotencyStore);
    }

    private static <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> register(T filter, int order) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(order);
        registration.addUrlPatterns("/*");
        return registration;
    }

    /** Scheduled cleanup, extracted so {@code @Scheduled} lands on a plain bean. */
    public static class InMemoryStoreEviction {

        private final InMemoryRateLimiter rateLimiter;
        private final InMemoryIdempotencyStore idempotencyStore;

        InMemoryStoreEviction(InMemoryRateLimiter rateLimiter, InMemoryIdempotencyStore idempotencyStore) {
            this.rateLimiter = rateLimiter;
            this.idempotencyStore = idempotencyStore;
        }

        @Scheduled(fixedDelayString = "PT5M")
        public void evict() {
            rateLimiter.evictExpired();
            idempotencyStore.evictExpired();
        }
    }
}
