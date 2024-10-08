plugins {
    kotlin("jvm")
}

dependencies {
    api(libs.bundles.jackson)
    api(libs.navfelles.rapidandriversktor2) {
        exclude("io.ktor", "ktor-server-cio")
        exclude("io.ktor", "ktor-server-metrics-micrometer")
    }
    api(libs.ktor2.servercio)
    api(libs.ktor2.metricsmicrometer)
    // Desse to over er spesifisert som api i r&r sjølv, så vi må ha dei med her for å ikkje få feil i runtime

    implementation(project(":libs:saksbehandling-common"))

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.mockk)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
}
