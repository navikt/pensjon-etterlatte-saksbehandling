plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-regler"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-trygdetid-model"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-jobs"))

    implementation(libs.database.kotliquery)
    implementation(libs.navfelles.tokenclientcore)

    testImplementation(libs.ktor.servertests)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.kotest.assertionscore)

    testImplementation(libs.ktor.jackson)
    testImplementation(libs.ktor.clientcontentnegotiation)
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
    testImplementation(testFixtures((project(":libs:etterlatte-funksjonsbrytere"))))
}
