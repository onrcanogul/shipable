package dev.onrcanogul.appbackend.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.cache.api.port.CacheService;
import dev.onrcanogul.appbackend.cache.internal.redis.NoopCacheService;
import dev.onrcanogul.appbackend.cache.internal.redis.RedisCacheService;
import dev.onrcanogul.appbackend.core.api.port.IdempotencyStore;
import dev.onrcanogul.appbackend.core.api.port.RateLimiter;
import dev.onrcanogul.appbackend.core.api.settings.SettingCatalog;
import dev.onrcanogul.appbackend.core.api.settings.SettingDefinition;
import dev.onrcanogul.appbackend.core.api.support.InMemoryIdempotencyStore;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.support.InMemoryRateLimiter;
import dev.onrcanogul.appbackend.core.api.support.PropertiesRuntimeSettings;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * The behaviour that matters here is the switch: Redis off is the default, and turning it
 * on must replace the rate limiter and idempotency store without anyone else noticing.
 *
 * <p>No Redis server is started. A mocked connection factory is enough to prove the wiring;
 * the Redis implementations are exercised against a real server in the host integration
 * test.
 */
class CacheModuleContextTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(InMemoryRateLimiter.class, () -> new InMemoryRateLimiter(Clock.systemUTC()))
            .withBean(InMemoryIdempotencyStore.class, () -> new InMemoryIdempotencyStore(Clock.systemUTC()))
            // No stored overrides, so every setting falls back to app.cache.* - the state a
            // fresh deployment is in.
            .withBean(RuntimeSettings.class, PropertiesRuntimeSettings::new)
            .withUserConfiguration(CacheModuleConfiguration.class);

    @Test
    @DisplayName("with Redis off, the cache is a no-op and the in-memory stores stand")
    void redisOffByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(CacheService.class)).isInstanceOf(NoopCacheService.class);
            assertThat(context).doesNotHaveBean(RedisCacheService.class);
        });
    }

    @Test
    @DisplayName("a no-op cache never caches, so getOrCompute always calls the loader")
    void noopCacheAlwaysComputes() {
        runner.run(context -> {
            CacheService cache = context.getBean(CacheService.class);
            cache.put("k", "v", Duration.ofMinutes(1));

            assertThat(cache.get("k", String.class)).isEmpty();
            assertThat(cache.getOrCompute("k", String.class, Duration.ofMinutes(1), () -> "computed"))
                    .isEqualTo("computed");
        });
    }

    @Test
    @DisplayName("turning Redis on replaces the cache, rate limiter and idempotency store")
    void redisTakesOverWhenEnabled() {
        runner.withPropertyValues("app.cache.enabled=true")
                .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
                .withBean(RedisConnectionFactory.class, () -> Mockito.mock(RedisConnectionFactory.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CacheService.class)).isInstanceOf(RedisCacheService.class);
                    // @Primary wins for the interface; the in-memory beans stay as fallbacks.
                    assertThat(context.getBean(RateLimiter.class)).isNotInstanceOf(InMemoryRateLimiter.class);
                    assertThat(context.getBean(IdempotencyStore.class))
                            .isNotInstanceOf(InMemoryIdempotencyStore.class);
                    assertThat(context).hasSingleBean(InMemoryRateLimiter.class);
                });
    }

    @Test
    @DisplayName("the module declares its runtime settings, and only the ones that can work")
    void declaresItsRuntimeSettings() {
        runner.run(context -> {
            var keys = context.getBean(SettingCatalog.class).definitions().stream()
                    .map(SettingDefinition::key)
                    .toList();

            assertThat(keys).containsExactlyInAnyOrder("cache.bypass", "cache.default-ttl");
            // Absent on purpose: both need a restart, so offering them would be a switch
            // that looks like it works and does nothing.
            assertThat(keys).doesNotContain("app.cache.enabled", "spring.data.redis.host");
        });
    }

    @Test
    @DisplayName("the catalog reports this deployment's configured TTL as the default")
    void catalogShowsConfiguredTtl() {
        runner.withPropertyValues("app.cache.default-ttl=45s").run(context -> {
            var ttl = context.getBean(SettingCatalog.class).definitions().stream()
                    .filter(definition -> definition.key().equals("cache.default-ttl"))
                    .findFirst()
                    .orElseThrow();

            assertThat(ttl.bootDefault()).isEqualTo(Duration.ofSeconds(45).toString());
        });
    }

    @Test
    @DisplayName("keys are namespaced so one Redis can serve dev and prod")
    void keysAreNamespaced() {
        runner.withPropertyValues("app.cache.key-prefix=myapp-prod").run(context -> {
            CacheProperties properties = context.getBean(CacheProperties.class);
            assertThat(properties.key("ratelimit", "1.2.3.4")).isEqualTo("myapp-prod:ratelimit:1.2.3.4");
        });
    }
}
