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
    implementation(libs.ktor.clientcore)

    implementation(libs.etterlatte.common)
    implementation(libs.pdf.pdfbox)

    implementation(libs.brevbaker.api.model.common)

    implementation(libs.database.kotliquery)
    implementation(libs.cache.caffeine)

    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.okhttp)
    testImplementation(libs.ktor.servertests)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures((project(":libs:etterlatte-funksjonsbrytere"))))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
