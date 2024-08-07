plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(libs.etterlatte.common)

    implementation(project(":libs:etterlatte-ktor"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.clientciojvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientlogging)
    implementation(libs.ktor2.jackson)

    implementation(libs.bundles.jackson)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.test.kotest.assertionscore)
}
