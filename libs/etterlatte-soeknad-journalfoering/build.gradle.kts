plugins {
    kotlin("jvm")
    id("java-library")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(libs.etterlatte.common)
    implementation(libs.ktor.clientcore)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logging.slf4japi)

    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.clientcontentnegotiation)
    testImplementation(libs.ktor.jackson)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
}
