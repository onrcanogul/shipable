package dev.onrcanogul.appbackend.host;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The module boundaries, as a test rather than as a paragraph nobody rereads.
 *
 * <p>Maven already enforces the coarse direction — the enforcer in {@code platform/pom.xml}
 * breaks the build if a platform module declares a dependency on {@code domain}. These
 * rules cover what Maven cannot see: which <i>packages</i> may be reached inside a module
 * that is legitimately on the classpath.
 *
 * <p>Test classes are excluded. A test reaching into its own module's internals is normal
 * and useful; production code doing it is the thing being prevented.
 */
class ArchitectureRulesTest {

    private static final String BASE = "dev.onrcanogul.appbackend";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    @DisplayName("platform modules never depend on the domain module")
    void platformDoesNotDependOnDomain() {
        noClasses()
                .that().resideInAPackage(BASE + ".(core|cache|identity|billing|quota|notifications|analytics|appconfig|privacy|admin)..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".domain..")
                .because("the platform must stay reusable: you delete your app and it still stands")
                .check(classes);
    }

    @Test
    @DisplayName("core depends on no other module")
    void coreDependsOnNothing() {
        noClasses()
                .that().resideInAPackage(BASE + ".core..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE + ".cache..",
                        BASE + ".identity..",
                        BASE + ".billing..",
                        BASE + ".quota..",
                        BASE + ".notifications..",
                        BASE + ".analytics..",
                        BASE + ".appconfig..",
                        BASE + ".privacy..",
                        BASE + ".admin..",
                        BASE + ".domain..",
                        BASE + ".host..")
                .because("core is the floor; everything stands on it and it stands on nothing")
                .check(classes);
    }

    @Test
    @DisplayName("a module's internal package is reachable only from that module")
    void internalPackagesAreClosed() {
        for (String module : new String[] {
                "core", "cache", "identity", "billing", "quota", "notifications", "analytics", "appconfig",
                "privacy", "admin", "domain"}) {
            noClasses()
                    .that().resideOutsideOfPackage(BASE + "." + module + "..")
                    .should().dependOnClassesThat().resideInAPackage(BASE + "." + module + ".internal..")
                    .because(module + "'s internals are its own; other modules use its api package")
                    .check(classes);
        }
    }

    @Test
    @DisplayName("nothing depends on the host module")
    void hostIsALeaf() {
        noClasses()
                .that().resideOutsideOfPackage(BASE + ".host..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".host..")
                .because("the host only wires modules together; a module needing it would be a cycle")
                .check(classes);
    }

    @Test
    @DisplayName("no module depends on the admin module")
    void adminIsALeaf() {
        noClasses()
                .that().resideOutsideOfPackage(BASE + ".admin..")
                // The host imports every module's configuration by name; that is the wiring,
                // not a dependency. Every other module must be able to compile without admin.
                .and().resideOutsideOfPackage(BASE + ".host..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".admin..")
                .because("the admin API must be deletable: it is the most dangerous surface here, "
                        + "and nothing should stop you removing it")
                .check(classes);
    }

    @Test
    @DisplayName("Redis is confined to the cache module")
    void onlyCacheKnowsAboutRedis() {
        noClasses()
                .that().resideOutsideOfPackage(BASE + ".cache..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.data.redis..")
                .because("Redis is optional; a module that imported it directly would make it mandatory")
                .check(classes);
    }

    @Test
    @DisplayName("billing is the only module that talks to RevenueCat")
    void billingOwnsTheProviderIntegration() {
        noClasses()
                .that().resideOutsideOfPackage(BASE + ".billing..")
                .should().dependOnClassesThat().haveNameMatching(".*RevenueCat.*")
                .because("replacing the billing provider should touch one module, not the whole app")
                .check(classes);
    }
}
