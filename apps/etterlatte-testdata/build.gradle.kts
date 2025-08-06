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
    implementation(libs.ktor2.mustache)

    implementation(libs.cache.caffeine)
    implementation(libs.etterlatte.common)

    implementation(libs.navfelles.tokenvalidationktor2) {
        exclude("io.ktor", "ktor-server")
    }
    implementation(libs.ktor2.server)
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-swagger:3.2.3")

    testImplementation(libs.test.kotest.assertionscore)

    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(libs.ktor2.jackson)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
