package dev.onrcanogul.appbackend.domain;

import org.springframework.context.annotation.Configuration;

/**
 * Everything your app hands to the outside world.
 *
 * <p>This is the one module the template leaves empty. Everything else exists to serve it.
 *
 * <p>Declare your beans here explicitly rather than component-scanning {@code internal}, the
 * same way the platform modules do. It costs a line per bean and buys a single file that
 * says what this module contains.
 *
 * <p>Two beans worth defining early:
 * <ul>
 *   <li>a {@code QuotaPolicy}, or every metered call is denied - the platform deliberately
 *       ships no default limits;</li>
 *   <li>a {@code UserDataContributor} for each table you add that holds user data, so
 *       account deletion and export keep working.</li>
 * </ul>
 *
 * <p>See {@code docs/BUILDING-YOUR-APP.md} for the walkthrough.
 */
@Configuration(proxyBeanMethods = false)
public class DomainModuleConfiguration {

    // Your beans go here.
}
