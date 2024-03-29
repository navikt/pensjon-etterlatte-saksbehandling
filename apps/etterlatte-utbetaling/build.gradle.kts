plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.common")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-jobs"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-utbetaling-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-mq"))
    implementation(project(":libs:etterlatte-migrering-model"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.jackson)

    implementation(libs.mq.jakarta.client)
    implementation(libs.messaginghub.pooled.jms)
    implementation(libs.navfelles.tjenestespesifikasjoner.oppdragsbehandling)
    implementation(libs.navfelles.tjenestespesifikasjoner.avstemming)

    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.wiremock)
    testImplementation(project(":libs:testdata"))
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures(project(":libs:etterlatte-mq")))
}
