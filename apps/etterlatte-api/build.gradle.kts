plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-beregning-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-sporingslogg"))

    implementation("io.ktor:ktor-server-swagger:3.2.2")

    testImplementation(libs.ktor.clientcontentnegotiation)
    testImplementation(libs.ktor.jackson)
    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.servertests)
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
