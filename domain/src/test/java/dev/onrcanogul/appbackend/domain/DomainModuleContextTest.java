package dev.onrcanogul.appbackend.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * The module is empty by design, so this test only proves the wiring point exists.
 *
 * <p>Keep it as your app grows: it stays the fastest signal that the module still loads on
 * its own, without a database or the rest of the application.
 */
class DomainModuleContextTest {

    @Test
    @DisplayName("module configuration loads")
    void contextLoads() {
        new ApplicationContextRunner()
                .withUserConfiguration(DomainModuleConfiguration.class)
                .run(context -> assertThat(context).hasNotFailed());
    }
}
