package dev.onrcanogul.appbackend.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.core.api.port.IdempotencyStore;
import dev.onrcanogul.appbackend.core.api.port.RateLimiter;
import dev.onrcanogul.appbackend.core.internal.web.GlobalExceptionHandler;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/**
 * Does the module's context load?
 *
 * <p>No database is started on purpose: core is a library. Schema and Flyway are exercised
 * by the Testcontainers test in the host module.
 */
class CoreModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withPropertyValues("app.core.rate-limit.permits=120", "app.core.rate-limit.window=1m")
            .withUserConfiguration(CoreModuleConfiguration.class);

    @Test
    @DisplayName("module configuration loads and exposes the request pipeline")
    void contextLoads() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context).hasSingleBean(RateLimiter.class);
            assertThat(context).hasSingleBean(IdempotencyStore.class);
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
        });
    }

    @Test
    @DisplayName("filters are registered in the order the pipeline depends on")
    void filtersAreOrdered() {
        runner.run(context -> {
            var registrations = context.getBeansOfType(FilterRegistrationBean.class).values().stream()
                    .sorted(java.util.Comparator.comparingInt(FilterRegistrationBean::getOrder))
                    .map(registration -> registration.getFilter().getClass().getSimpleName())
                    .toList();

            assertThat(registrations)
                    .containsExactly("RequestContextBindingFilter", "RateLimitFilter", "IdempotencyFilter");
        });
    }

    @Test
    @DisplayName("rate limit settings are read from configuration")
    void rateLimitIsConfigurable() {
        runner.withPropertyValues("app.core.rate-limit.permits=7", "app.core.rate-limit.window=30s")
                .run(context -> {
                    CoreProperties properties = context.getBean(CoreProperties.class);
                    assertThat(properties.rateLimit().permits()).isEqualTo(7);
                    assertThat(properties.rateLimit().window()).hasSeconds(30);
                });
    }
}
