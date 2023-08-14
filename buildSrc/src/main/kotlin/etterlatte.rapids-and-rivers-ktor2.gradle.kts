import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("jvm") apply false
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.navfelles.rapidandriversktor2)
    implementation(project(":libs:rapidsandrivers-extras"))
}