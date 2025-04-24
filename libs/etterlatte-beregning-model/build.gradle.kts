plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-regler"))
    implementation(project(":libs:etterlatte-inntektsjustering-model"))

    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(libs.test.kotest.assertionscore)
}
