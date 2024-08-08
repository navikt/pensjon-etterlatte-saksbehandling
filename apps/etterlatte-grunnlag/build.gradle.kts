plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-sporingslogg"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-pdl-model"))
    implementation(project(":libs:etterlatte-migrering-model"))

    implementation(libs.database.kotliquery)
    implementation(libs.cache.caffeine)

    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures(project(":libs:etterlatte-database")))
    testImplementation(testFixtures(project(":libs:etterlatte-ktor")))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
