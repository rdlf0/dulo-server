package com.dulo.service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Guards the hexagonal architecture: the domain model at the center, adapters at the edges. */
@AnalyzeClasses(packages = "com.dulo", importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsPlainJava =
            classes()
                    .that()
                    .resideInAPackage("com.dulo.domain..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage("com.dulo.domain..", "java..", "org.jspecify..")
                    .because("the domain must not depend on frameworks or outer layers");

    @ArchTest
    static final ArchRule coreDependsOnlyOnDomain =
            classes()
                    .that()
                    .resideInAPackage("com.dulo.core..")
                    .should()
                    .onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "com.dulo.core..", "com.dulo.domain..", "java..", "org.jspecify..")
                    .because("use cases must stay framework-free");

    @ArchTest
    static final ArchRule layers =
            layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Domain")
                    .definedBy("com.dulo.domain..")
                    .layer("Core")
                    .definedBy("com.dulo.core..")
                    .layer("Adapters")
                    .definedBy("com.dulo.adapter..")
                    .layer("Service")
                    .definedBy("com.dulo.service..")
                    .whereLayer("Service")
                    .mayNotBeAccessedByAnyLayer()
                    .whereLayer("Adapters")
                    .mayOnlyBeAccessedByLayers("Service")
                    .whereLayer("Core")
                    .mayOnlyBeAccessedByLayers("Adapters", "Service");

    @ArchTest
    static final ArchRule adaptersAreIndependent =
            slices().matching("com.dulo.adapter.(*)..").should().notDependOnEachOther();

    @ArchTest
    static final ArchRule adaptersUsePortsOnly =
            noClasses()
                    .that()
                    .resideInAPackage("com.dulo.adapter..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.dulo.core..service..")
                    .because("adapters reach the core only through ports");

    @ArchTest
    static final ArchRule restAdapterUsesInboundPortsOnly =
            noClasses()
                    .that()
                    .resideInAPackage("com.dulo.adapter.rest..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.dulo.core..port.out..");

    @ArchTest
    static final ArchRule jpaStaysInPersistenceAdapter =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("com.dulo.adapter.persistence.jpa..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("jakarta.persistence..", "org.hibernate..");

    @ArchTest
    static final ArchRule jpaEntityNaming =
            classes()
                    .that()
                    .areAnnotatedWith("jakarta.persistence.Entity")
                    .should()
                    .haveSimpleNameEndingWith("JpaEntity")
                    .andShould()
                    .notBePublic();

    @ArchTest
    static final ArchRule inboundPortNaming =
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
    static final ArchRule outboundPortNaming =
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
    static final ArchRule controllers =
            classes()
                    .that()
                    .areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should()
                    .resideInAPackage("com.dulo.adapter.rest..")
                    .andShould()
                    .haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule noPackageCycles =
            slices().matching("com.dulo.(**)").should().beFreeOfCycles();

    private ArchitectureTest() {}
}
