package dev.onrcanogul.appbackend.admin.internal.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IpAllowListTest {

    @Test
    @DisplayName("an empty list allows everything - the key is then the only lock")
    void emptyListAllowsAll() {
        assertThat(IpAllowList.permits(List.of(), "203.0.113.9")).isTrue();
        assertThat(IpAllowList.permits(null, "203.0.113.9")).isTrue();
    }

    @Test
    @DisplayName("exact addresses match only themselves")
    void exactMatch() {
        List<String> allowed = List.of("203.0.113.9");

        assertThat(IpAllowList.permits(allowed, "203.0.113.9")).isTrue();
        assertThat(IpAllowList.permits(allowed, "203.0.113.10")).isFalse();
    }

    @Test
    @DisplayName("a CIDR range matches inside it and nothing outside")
    void cidrRange() {
        List<String> allowed = List.of("203.0.113.0/24");

        assertThat(IpAllowList.permits(allowed, "203.0.113.1")).isTrue();
        assertThat(IpAllowList.permits(allowed, "203.0.113.255")).isTrue();
        assertThat(IpAllowList.permits(allowed, "203.0.114.1")).isFalse();
    }

    @Test
    @DisplayName("a /32 is a single address, not a range")
    void singleHostCidr() {
        assertThat(IpAllowList.permits(List.of("10.0.0.5/32"), "10.0.0.5")).isTrue();
        assertThat(IpAllowList.permits(List.of("10.0.0.5/32"), "10.0.0.6")).isFalse();
    }

    @Test
    @DisplayName("a malformed entry denies rather than quietly allowing everything")
    void malformedEntryDenies() {
        assertThat(IpAllowList.permits(List.of("not-an-ip/24"), "10.0.0.1")).isFalse();
        assertThat(IpAllowList.permits(List.of("10.0.0.0/999"), "10.0.0.1")).isFalse();
    }

    @Test
    @DisplayName("a missing caller address is denied when a list is configured")
    void unknownCallerDenied() {
        assertThat(IpAllowList.permits(List.of("10.0.0.0/8"), null)).isFalse();
        assertThat(IpAllowList.permits(List.of("10.0.0.0/8"), "")).isFalse();
    }

    @Test
    @DisplayName("IPv4-mapped IPv6 loopback compares as IPv4")
    void ipv4MappedLoopback() {
        assertThat(IpAllowList.permits(List.of("127.0.0.1"), "::ffff:127.0.0.1")).isTrue();
    }

    @Test
    @DisplayName("an IPv6 entry with a prefix is an exact match, never a widened range")
    void ipv6PrefixIsNotWidened() {
        assertThat(IpAllowList.permits(List.of("2001:db8::/32"), "2001:db8::1")).isFalse();
        assertThat(IpAllowList.permits(List.of("2001:db8::1"), "2001:db8::1")).isTrue();
    }
}
