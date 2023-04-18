plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.clientcore)

    testImplementation(libs.test.mockk)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(project(":libs:testdata"))

    implementation(":apps:etterlatte-vilkaarsvurdering")
    implementation(":apps:etterlatte-vilkaarsvurdering-kafka")
}