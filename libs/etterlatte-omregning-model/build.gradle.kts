plugins {
    kotlin("jvm")
}

dependencies {
    api(libs.bundles.jackson)
    api(libs.navfelles.rapidandriversktor2) {
        exclude("io.ktor", "ktor-server-cio")
        exclude("io.ktor", "ktor-server-metrics-micrometer")
    }

    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-inntektsjustering-model"))

    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
}
