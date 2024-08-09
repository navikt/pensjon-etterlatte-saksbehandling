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
    implementation(libs.ktor2.mustache)

    implementation(libs.cache.caffeine)
    implementation(libs.etterlatte.common)

    implementation(libs.navfelles.tokenvalidationktor2) {
        exclude("io.ktor", "ktor-server")
    }
    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.statuspages) // For å kompensere for exclude-en over

    testImplementation(libs.test.kotest.assertionscore)
}
