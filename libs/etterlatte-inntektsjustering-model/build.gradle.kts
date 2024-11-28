plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))

    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.jupiter.params)
}
