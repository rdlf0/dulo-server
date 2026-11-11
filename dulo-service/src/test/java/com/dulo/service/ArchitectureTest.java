package com.dulo.service;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Guards the hexagonal architecture: the domain model at the center, adapters at the edges. */
@SuppressWarnings("unused")
@AnalyzeClasses(packages = "com.dulo", importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureTest {

    @ArchTest
    static final ArchRule DOMAIN_IS_PLAIN_JAVA =
            classes()
                    .that()
                    .resideInAPackage("com.dulo.domain..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage("com.dulo.domain..", "java..", "org.jspecify..")
                    .because("the domain must not depend on frameworks or outer layers");

    @ArchTest
    static final ArchRule CORE_DEPENDS_ONLY_ON_DOMAIN =
            classes()
                    .that()
                    .resideInAPackage("com.dulo.core..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "com.dulo.core..", "com.dulo.domain..", "java..", "org.jspecify..")
                    .because("use cases must stay framework-free");

    @ArchTest
    static final ArchRule ONION_ARCHITECTURE =
            onionArchitecture()
                    .domainModels("com.dulo.domain..")
                    .applicationServices("com.dulo.core..")
                    .adapter("persistence", "com.dulo.adapter.persistence..")
                    .adapter("rest", "com.dulo.adapter.rest..")
                    .withOptionalLayers(true)
                    // The service module is the composition root: it wires all layers together.
                    .ignoreDependency(resideInAPackage("com.dulo.service.."), alwaysTrue());

    @ArchTest
    static final ArchRule ADAPTERS_ARE_INDEPENDENT =
            slices().matching("com.dulo.adapter.(*)..").should().notDependOnEachOther();

    @ArchTest
    static final ArchRule ADAPTERS_USE_PORTS_ONLY =
            noClasses()
                    .that()
                    .resideInAPackage("com.dulo.adapter..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.dulo.core..service..")
                    .because("adapters reach the core only through ports");

    @ArchTest
    static final ArchRule REST_ADAPTER_USES_INBOUND_PORTS_ONLY =
            noClasses()
                    .that()
                    .resideInAPackage("com.dulo.adapter.rest..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.dulo.core..port.out..");

    @ArchTest
    static final ArchRule JPA_STAYS_IN_PERSISTENCE_ADAPTER =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("com.dulo.adapter.persistence.jpa..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "org.hibernate..");

    @ArchTest
    static final ArchRule JPA_ENTITY_NAMING =
            classes()
                    .that()
                    .areAnnotatedWith("jakarta.persistence.Entity")
                    .should()
                    .haveSimpleNameEndingWith("JpaEntity")
                    .andShould()
                    .notBePublic();

    @ArchTest
    static final ArchRule INBOUND_PORT_NAMING =
            classes()
                    .that()
                    .resideInAPackage("com.dulo.core..port.in..")
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .and()
                    .areInterfaces()
                    .should()
                    .haveSimpleNameEndingWith("UseCase");

    @ArchTest
    static final ArchRule OUTBOUND_PORT_NAMING =
            classes()
                    .that()
                    .resideInAPackage("com.dulo.core..port.out..")
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .should()
                    .beInterfaces()
                    .andShould()
                    .haveSimpleNameEndingWith("Repository")
                    .orShould()
                    .haveSimpleNameEndingWith("Port")
                    .orShould()
                    .haveSimpleName("UnitOfWork");

    @ArchTest
    static final ArchRule CONTROLLERS =
            classes()
                    .that()
                    .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should()
                    .resideInAPackage("com.dulo.adapter.rest..")
                    .andShould()
                    .haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule NO_PACKAGE_CYCLES =
            slices().matching("com.dulo.(**)").should().beFreeOfCycles();

    private ArchitectureTest() {}
}
