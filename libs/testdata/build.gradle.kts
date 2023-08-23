plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

repositories {
    maven("https://github.com")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(libs.etterlatte.common)

    testImplementation(libs.test.jupiter.engine)
}