import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("jvm") apply false
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.database.postgresql)
    implementation(libs.database.flywaydb)
    implementation(libs.database.hikaricp)

    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)
}