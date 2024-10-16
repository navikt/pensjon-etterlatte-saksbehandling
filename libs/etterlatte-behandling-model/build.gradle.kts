plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(libs.etterlatte.common)
    implementation(project(mapOf("path" to ":libs:etterlatte-tidshendelser-model")))
    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.jupiter.params)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
