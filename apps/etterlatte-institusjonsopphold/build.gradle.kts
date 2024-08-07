plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-institusjonsopphold-model"))

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
}
