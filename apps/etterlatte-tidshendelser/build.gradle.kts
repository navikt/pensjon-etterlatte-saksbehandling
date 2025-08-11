plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-tidshendelser-model"))
    implementation(project(":libs:etterlatte-inntektsjustering-model"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-jobs"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures(project(":libs:etterlatte-database")))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
    testImplementation(libs.test.navfelles.rapidsandriversktor)
}
