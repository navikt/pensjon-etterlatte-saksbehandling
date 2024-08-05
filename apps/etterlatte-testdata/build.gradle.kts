plugins {
    id("etterlatte.common")
}

dependencies {
    api(kotlin("reflect"))

    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.metricsmicrometer)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.mustache)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.statuspages)

    implementation(libs.bundles.jackson)
    implementation(libs.jackson.databind)

    implementation(libs.etterlatte.common)

    implementation(libs.navfelles.tokenclientcore)
    implementation(libs.navfelles.tokenvalidationktor2)

    testImplementation(libs.test.kotest.assertionscore)
}
