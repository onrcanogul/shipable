package dev.onrcanogul.appbackend.host.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Finds the entities and repositories that live in the modules.
 *
 * <p>Kept out of {@link dev.onrcanogul.appbackend.host.Application} on purpose: a web slice
 * test ({@code @WebMvcTest}) excludes plain {@code @Configuration} classes but not the
 * application class, so putting these annotations there would drag JPA into every
 * controller test and force each one to start a database.
 *
 * <p>Scanning the whole base package is fine here even though it crosses into
 * {@code internal}: this is infrastructure discovery, not a dependency. No module gains
 * access to another module's classes because of it.
 */
@Configuration(proxyBeanMethods = false)
@EnableTransactionManagement
@EntityScan(basePackages = "dev.onrcanogul.appbackend")
@EnableJpaRepositories(basePackages = "dev.onrcanogul.appbackend")
public class JpaConfiguration {
}
