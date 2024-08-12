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

    implementation(libs.navfelles.tokenvalidationktor2)

    testImplementation(libs.test.kotest.assertionscore)
}
