plugins {
    id("etterlatte.common")
}

dependencies {
    api(kotlin("reflect"))

    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(libs.jackson.core)
    implementation(libs.bundles.jackson)
}
