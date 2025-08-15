plugins {
    id("etterlatte.common")
}

dependencies {
    api(kotlin("reflect"))

    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-omregning-model"))
    implementation(project(":libs:rapidsandrivers-extras"))
    implementation(libs.ktor.mustache)

    implementation(libs.cache.caffeine)
    implementation(libs.etterlatte.common)

    implementation(libs.navfelles.tokenvalidationktor) {
        exclude("io.ktor", "ktor-server")
    }
    implementation(libs.ktor.server)
    implementation(libs.ktor.servercorejvm)
    implementation(libs.ktor.serverswagger)

    testImplementation(libs.test.kotest.assertionscore)

    testImplementation(libs.ktor.clientcontentnegotiation)
    testImplementation(libs.ktor.jackson)
    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.servertests)
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
    testImplementation(libs.test.navfelles.rapidsandriversktor)
}
