plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.ktor.clientcore)
    implementation(libs.ktor.clientcontentnegotiation)
    implementation(libs.ktor.jackson)
    implementation(project(":libs:saksbehandling-common"))

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.test.mockk)
}
