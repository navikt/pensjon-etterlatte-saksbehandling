plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.unleash.client)
    implementation(project(":libs:saksbehandling-common"))
}