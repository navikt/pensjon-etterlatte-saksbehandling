plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-trygdetid-model"))
    implementation(project(":libs:rapidsandrivers-extras"))
    implementation(project(":libs:etterlatte-migrering-model"))
    implementation(project(":libs:rapidsandrivers-extras"))

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
}
