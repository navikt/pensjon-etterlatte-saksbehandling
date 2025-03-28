plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-beregning-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-trygdetid-model"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))
    implementation(project(":libs:etterlatte-tilbakekreving-model"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:etterlatte-brev-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(libs.ktor2.clientcore)

    implementation(libs.etterlatte.common)
    implementation(libs.pdf.pdfbox)

    implementation("no.nav.pensjon.brevbaker:brevbaker-api-model-common:1.4.0")

    implementation(libs.database.kotliquery)
    implementation(libs.cache.caffeine)

    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.okhttp)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures((project(":libs:etterlatte-funksjonsbrytere"))))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
