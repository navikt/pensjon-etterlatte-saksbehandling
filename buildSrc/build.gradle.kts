import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.20"
    `kotlin-dsl`
}

group = "no.nav.etterlatte"
version = "unspecified"

repositories {
    gradlePluginPortal()
    maven(
        // name = "JCenter Gradle Plugins",
        url = "https://dl.bintray.com/gradle/gradle-plugins",
    )
}

dependencies {
    implementation(kotlin("gradle-plugin"))

    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

tasks {
    withType<Wrapper> {
        gradleVersion = "8.10"
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
