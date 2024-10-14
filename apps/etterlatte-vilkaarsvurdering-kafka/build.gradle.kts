plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:etterlatte-ktor")) {
        exclude("io.ktor:ktor-server-cio", "io.ktor:ktor-server-metrics-micrometer")
    }
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))
    implementation(project(":libs:etterlatte-migrering-model"))
    implementation(project(":libs:rapidsandrivers-extras"))

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
