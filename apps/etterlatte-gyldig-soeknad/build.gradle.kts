plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(libs.etterlatte.common)

    implementation(project(":libs:etterlatte-ktor"))

    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.clientauth)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.ktor2.clientcontentnegotiation)
}
