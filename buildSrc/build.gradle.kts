import ca.cutterslade.gradle.analyze.AnalyzeDependenciesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    alias(libs.plugins.cutterslade.analyze) apply true
}

group = "no.nav.etterlatte"
version = "unspecified"

repositories {
    gradlePluginPortal()
    maven(
        // name = "JCenter Gradle Plugins",
        url = "https://dl.bintray.com/gradle/gradle-plugins"
    )
}

dependencies {
    implementation(kotlin("gradle-plugin"))

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

tasks {
    withType<Wrapper> {
        gradleVersion = "8.0"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
    }
    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    withType<AnalyzeDependenciesTask> {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
    }
}