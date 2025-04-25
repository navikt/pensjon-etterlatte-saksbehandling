plugins {
    kotlin("jvm")
}

dependencies {
    api(libs.jackson.datatypejsr310)
    api(libs.jackson.modulekotlin)

    implementation(project(":libs:saksbehandling-common"))
    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(libs.test.kotest.assertionscore)
}
