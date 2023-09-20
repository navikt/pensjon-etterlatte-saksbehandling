plugins {
    id("etterlatte.common")
}

dependencies {
    api(kotlin("reflect"))

    implementation(project(":libs:etterlatte-kafka"))

    implementation(libs.jackson.core)
    implementation(libs.bundles.jackson)
}
