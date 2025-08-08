plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-trygdetid-model"))
    implementation(project(":libs:rapidsandrivers-extras"))
    implementation(project(":libs:etterlatte-omregning-model"))
    implementation(project(":libs:rapidsandrivers-extras"))

    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.servertests)
}
