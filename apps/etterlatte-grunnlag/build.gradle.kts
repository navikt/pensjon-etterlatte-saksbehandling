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
    implementation(libs.etterlatte.common)

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientciojvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientlogging)
    implementation(libs.ktor2.metricsmicrometer)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.calllogging)

    implementation(libs.bundles.jackson)

    implementation(libs.navfelles.tokenclientcore)
    implementation(libs.navfelles.tokenvalidationktor2)

    implementation(libs.database.kotliquery)

    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(libs.ktor2.servertests)
    testImplementation(project(":libs:testdata"))
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures(project(":libs:etterlatte-database")))
    testImplementation(testFixtures(project(":libs:etterlatte-ktor")))
}
