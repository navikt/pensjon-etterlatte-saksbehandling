plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.jackson)

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.jupiter.engine)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.mockk)
}
tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}