plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-pdl-model"))
    implementation(project(":libs:etterlatte-brev-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-inntektsjustering-model"))
    implementation(project(":libs:etterlatte-omregning-model"))
    implementation(project(":libs:rapidsandrivers-extras"))
    implementation(project(":libs:etterlatte-beregning-model"))
    implementation(project(":libs:etterlatte-tidshendelser-model"))
    implementation(libs.etterlatte.common)

    implementation("no.nav.pensjon.brevbaker:brevbaker-api-model-common:1.4.0")

    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:etterlatte-funksjonsbrytere"))))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
