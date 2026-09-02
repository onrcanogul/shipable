package dev.onrcanogul.appbackend.host.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs each module's migrations as its own Flyway instance.
 *
 * <p><b>Why not one Flyway with several locations.</b> Flyway merges every configured
 * location into a single ordered migration set, and two migrations with the same version
 * are a hard error:
 *
 * <pre>FlywayException: Found more than one migration with version 1</pre>
 *
 * Every module here starts at {@code V1__init.sql}, so the application would refuse to
 * start. Working around it by handing each module a reserved version range — core owns
 * 1.x, identity 2.x — would make a module's version numbers depend on where it sits in a
 * global scheme, which is exactly the cross-module coupling the rest of this codebase
 * avoids.
 *
 * <p><b>What this does instead.</b> One Flyway per module, each pointed at that module's
 * location and its own schema, with its own {@code flyway_schema_history} table inside it.
 * A module's versions are then its own business: {@code identity} can reach V12 without
 * knowing that {@code billing} exists. Dropping a module drops its schema and its history
 * together.
 *
 * <p>Registered as a {@link FlywayMigrationStrategy} rather than as loose beans so Spring
 * Boot's ordering still holds: the migration initializer runs before the
 * {@code EntityManagerFactory}, which is what lets {@code ddl-auto: validate} check a
 * schema that already exists.
 *
 * <p>Shared settings — the datasource, {@code clean-disabled}, placeholders — are inherited
 * from the auto-configured instance, so {@code spring.flyway.*} still means what it says.
 * Only the location and schema are overridden.
 */
@Configuration(proxyBeanMethods = false)
public class FlywayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfiguration.class);

    /**
     * Module migration directory, mapped to the schema that module owns.
     *
     * <p>Adding a module means one line here — the same kind of edit as adding it to
     * {@code @Import} in {@code Application}. Order is preserved but does not matter:
     * there are no foreign keys across module schemas, by design.
     */
    private static final Map<String, String> MODULE_SCHEMAS = new LinkedHashMap<>();

    static {
        MODULE_SCHEMAS.put("core", "core");
        MODULE_SCHEMAS.put("identity", "identity");
        MODULE_SCHEMAS.put("billing", "billing");
        MODULE_SCHEMAS.put("quota", "quota");
        MODULE_SCHEMAS.put("notifications", "notifications");
        MODULE_SCHEMAS.put("appconfig", "appconfig");
        MODULE_SCHEMAS.put("privacy", "privacy");
        // Your app's tables live in `app`, so a platform upgrade can never collide.
        MODULE_SCHEMAS.put("domain", "app");
    }

    @Bean
    public FlywayMigrationStrategy modularFlywayMigrationStrategy() {
        return autoConfigured -> MODULE_SCHEMAS.forEach((module, schema) -> {
            Flyway moduleFlyway = Flyway.configure()
                    // Everything from spring.flyway.* - datasource, clean-disabled,
                    // placeholders - then narrowed to this module.
                    .configuration(autoConfigured.getConfiguration())
                    .locations("classpath:db/migration/" + module)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .createSchemas(true)
                    .load();

            var result = moduleFlyway.migrate();
            if (result.migrationsExecuted > 0) {
                log.info("Applied {} migration(s) for module '{}' in schema '{}'",
                        result.migrationsExecuted, module, schema);
            }
        });
    }
}
