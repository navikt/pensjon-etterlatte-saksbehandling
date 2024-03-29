plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-pdl-model"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(libs.etterlatte.common)

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientloggingjvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.jackson)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(project(":libs:testdata"))
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
}
