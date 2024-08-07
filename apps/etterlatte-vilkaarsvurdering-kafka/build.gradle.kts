plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))
    implementation(project(":libs:etterlatte-migrering-model"))
    implementation(project(":libs:rapidsandrivers-extras"))

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
}
