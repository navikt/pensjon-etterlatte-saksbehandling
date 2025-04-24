plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.jackson)
    implementation(project(":libs:saksbehandling-common"))

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.mockk)
}
