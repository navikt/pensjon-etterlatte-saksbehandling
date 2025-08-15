
plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-institusjonsopphold-model"))
    implementation(project(":libs:rapidsandrivers-extras"))

    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.servertests)
    testImplementation(libs.test.navfelles.rapidsandriversktor)
}
