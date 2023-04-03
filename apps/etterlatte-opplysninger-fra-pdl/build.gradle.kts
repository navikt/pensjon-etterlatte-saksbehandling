plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientloggingjvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientcontentnegotiation)

    testImplementation(libs.test.jupiter.root)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.mockk)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(project(":libs:testdata"))
}