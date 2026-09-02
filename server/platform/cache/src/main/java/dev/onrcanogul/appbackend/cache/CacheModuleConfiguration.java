package dev.onrcanogul.appbackend.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.cache.api.port.CacheService;
import dev.onrcanogul.appbackend.cache.internal.CacheSettingCatalog;
import dev.onrcanogul.appbackend.cache.internal.redis.NoopCacheService;
import dev.onrcanogul.appbackend.cache.internal.redis.RedisCacheService;
import dev.onrcanogul.appbackend.cache.internal.redis.RedisIdempotencyStore;
import dev.onrcanogul.appbackend.cache.internal.redis.RedisRateLimiter;
import dev.onrcanogul.appbackend.core.api.port.IdempotencyStore;
import dev.onrcanogul.appbackend.core.api.support.InMemoryIdempotencyStore;
import dev.onrcanogul.appbackend.core.api.support.InMemoryRateLimiter;
import dev.onrcanogul.appbackend.core.api.port.RateLimiter;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.settings.SettingCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Everything the cache module hands to the outside world.
 *
 * <p>Redis is <b>off by default</b>. A fresh clone runs with no Redis container, and core's
 * in-memory rate limiter and idempotency store apply. Set
 * {@code app.cache.enabled=true} and the Redis versions take over — the same interfaces, so
 * nothing that uses them changes.
 *
 * <p>The Redis beans are {@code @Primary} rather than replacing core's: the in-memory ones
 * stay in the context as the fallback each Redis implementation uses when Redis is
 * unreachable.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheModuleConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true")
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true")
    public CacheService redisCacheService(
            StringRedisTemplate redis,
            ObjectMapper objectMapper,
            CacheProperties properties,
            RuntimeSettings settings) {
        return new RedisCacheService(redis, objectMapper, properties, settings);
    }

    /**
     * Present whether or not Redis is on, so callers never have to check.
     */
    /**
     * Declares the cache settings an operator can change while the application runs.
     *
     * <p>Registered whether or not Redis is on, so the admin listing does not change shape
     * depending on configuration.
     */
    @Bean
    public SettingCatalog cacheSettingCatalog(CacheProperties properties) {
        return new CacheSettingCatalog(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "false", matchIfMissing = true)
    public CacheService noopCacheService() {
        return new NoopCacheService();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true")
    public RateLimiter redisRateLimiter(
            StringRedisTemplate redis, CacheProperties properties, InMemoryRateLimiter fallback) {
        return new RedisRateLimiter(redis, properties, fallback);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "app.cache", name = "enabled", havingValue = "true")
    public IdempotencyStore redisIdempotencyStore(
            StringRedisTemplate redis, CacheProperties properties, InMemoryIdempotencyStore fallback) {
        return new RedisIdempotencyStore(redis, properties, fallback);
    }
}
