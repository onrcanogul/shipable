package dev.onrcanogul.appbackend.appconfig.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The bug this type exists to prevent: comparing versions as strings, where "1.10.0" sorts
 * before "1.9.0" and the force-update gate locks out your newest release.
 */
class AppVersionTest {

    @Test
    @DisplayName("1.10.0 is newer than 1.9.0, which string comparison gets wrong")
    void comparesNumericallyNotLexically() {
        assertThat(AppVersion.of("1.10.0").isOlderThan(AppVersion.of("1.9.0"))).isFalse();
        assertThat(AppVersion.of("1.9.0").isOlderThan(AppVersion.of("1.10.0"))).isTrue();
    }

    @Test
    @DisplayName("a missing patch number reads as .0")
    void patchIsOptional() {
        assertThat(AppVersion.of("2.1")).isEqualTo(new AppVersion(2, 1, 0));
    }

    @Test
    @DisplayName("build suffixes are ignored rather than rejected")
    void ignoresSuffixes() {
        assertThat(AppVersion.of("1.4.2-beta.3")).isEqualTo(new AppVersion(1, 4, 2));
        assertThat(AppVersion.of("1.4.2+build.77")).isEqualTo(new AppVersion(1, 4, 2));
    }

    @Test
    @DisplayName("something that is not a version parses to null rather than throwing")
    void unparseableIsNull() {
        assertThat(AppVersion.parseOrNull("not-a-version")).isNull();
        assertThat(AppVersion.parseOrNull(null)).isNull();
        assertThat(AppVersion.parseOrNull("")).isNull();
    }

    @Test
    @DisplayName("equal versions are not older than each other")
    void equalIsNotOlder() {
        assertThat(AppVersion.of("3.0.0").isOlderThan(AppVersion.of("3.0.0"))).isFalse();
    }
}
