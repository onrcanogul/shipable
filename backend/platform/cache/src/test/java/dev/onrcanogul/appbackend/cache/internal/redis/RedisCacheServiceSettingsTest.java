package dev.onrcanogul.appbackend.cache.internal.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.onrcanogul.appbackend.cache.CacheProperties;
import dev.onrcanogul.appbackend.core.api.settings.RuntimeSettings;
import dev.onrcanogul.appbackend.core.api.settings.SettingKeys;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * The "settings first, configuration as the default" behaviour, which is the whole point of
 * reading these two values per call rather than fixing them at startup.
 */
class RedisCacheServiceSettingsTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final CacheProperties properties = new CacheProperties(true, "app", Duration.ofMinutes(10));
    private final MutableSettings settings = new MutableSettings();

    private final RedisCacheService cache =
            new RedisCacheService(redis, new ObjectMapper(), properties, settings);

    RedisCacheServiceSettingsTest() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    @DisplayName("with nothing stored, the configured TTL applies")
    void fallsBackToConfiguredTtl() {
        cache.put("k", "v", null);

        verify(values).set(eq("app:cache:k"), any(), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("a stored default-ttl overrides the configured one")
    void settingOverridesConfiguredTtl() {
        settings.put(SettingKeys.CACHE_DEFAULT_TTL, "45s");

        cache.put("k", "v", null);

        verify(values).set(eq("app:cache:k"), any(), eq(Duration.ofSeconds(45)));
    }

    @Test
    @DisplayName("a caller's explicit TTL beats both")
    void explicitTtlWins() {
        settings.put(SettingKeys.CACHE_DEFAULT_TTL, "45s");

        cache.put("k", "v", Duration.ofSeconds(5));

        verify(values).set(eq("app:cache:k"), any(), eq(Duration.ofSeconds(5)));
    }

    @Test
    @DisplayName("a malformed stored TTL falls back rather than throwing")
    void malformedTtlFallsBack() {
        settings.put(SettingKeys.CACHE_DEFAULT_TTL, "not-a-duration");

        cache.put("k", "v", null);

        verify(values).set(eq("app:cache:k"), any(), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("the bypass switch turns reads into misses and writes into no-ops")
    void bypassMakesTheCacheInert() {
        settings.put(SettingKeys.CACHE_BYPASS, "true");

        cache.put("k", "v", Duration.ofMinutes(1));
        Optional<String> read = cache.get("k", String.class);
        String computed = cache.getOrCompute("k", String.class, Duration.ofMinutes(1), () -> "computed");

        assertThat(read).isEmpty();
        assertThat(computed).isEqualTo("computed");
        verify(values, never()).set(any(), any(), any(Duration.class));
        verify(values, never()).get(any());
    }

    @Test
    @DisplayName("eviction still runs while bypassed, so stale values do not survive it")
    void evictionIgnoresTheBypass() {
        settings.put(SettingKeys.CACHE_BYPASS, "true");

        cache.evict("k");

        verify(redis).delete("app:cache:k");
    }

    /** A settings source a test can change between calls, like the admin API would. */
    private static final class MutableSettings implements RuntimeSettings {
        private final Map<String, String> values = new HashMap<>();

        void put(String key, String value) {
            values.put(key, value);
        }

        @Override
        public Optional<String> find(String key) {
            return Optional.ofNullable(values.get(key));
        }
    }
}
