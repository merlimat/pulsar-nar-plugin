plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.maven.publish.plugin)
}

version = providers.environmentVariable("RELEASE_VERSION")
    .orElse(providers.exec {
        commandLine("git", "describe", "--tags", "--always")
        workingDir = rootDir
    }.standardOutput.asText.map { it.trim() })
    .map { it.removePrefix("v") }.get()

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testImplementation(gradleTestKit())
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Functional test source set
val functionalTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[functionalTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[functionalTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

val functionalTestTask = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTest.output.classesDirs
    classpath = functionalTest.runtimeClasspath
    useJUnitPlatform()
}

tasks.check {
    dependsOn(functionalTestTask)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    website.set("https://github.com/merlimat/pulsar-nar-plugin")
    vcsUrl.set("https://github.com/merlimat/pulsar-nar-plugin")

    plugins {
        create("nar") {
            id = "io.github.merlimat.nar"
            displayName = "Pulsar NAR Plugin"
            description = "Packages projects as NAR (NiFi Archive) files for Apache Pulsar IO connectors"
            tags.set(listOf("pulsar", "nar", "nifi", "packaging", "connector"))
            implementationClass = "io.github.merlimat.gradle.nar.NarPlugin"
        }
    }

    testSourceSets(functionalTest)
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    // Only sign when signing credentials are available (skip for local publishToMavenLocal)
    if (project.hasProperty("signing.keyId") ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null) {
        signAllPublications()
    }

    pom {
        name.set("Pulsar NAR Plugin")
        description.set("Packages projects as NAR (NiFi Archive) files for Apache Pulsar IO connectors")
        url.set("https://github.com/merlimat/pulsar-nar-plugin")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        developers {
            developer {
                id.set("merlimat")
                name.set("Matteo Merli")
                url.set("https://github.com/merlimat")
            }
        }

        scm {
            url.set("https://github.com/merlimat/pulsar-nar-plugin")
            connection.set("scm:git:https://github.com/merlimat/pulsar-nar-plugin.git")
            developerConnection.set("scm:git:git@github.com:merlimat/pulsar-nar-plugin.git")
        }
    }
}
