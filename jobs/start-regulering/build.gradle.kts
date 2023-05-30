plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    api(kotlin("reflect"))

    implementation(libs.logging.logbackclassic)
    implementation(libs.etterlatte.common)
    implementation(project(":libs:etterlatte-kafka"))
    implementation(libs.logging.logstashlogbackencoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.jackson.core)
    implementation(libs.bundles.jackson)

    testImplementation(libs.test.mockk)
}