plugins {
    id("etterlatte.libs")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.jupiter.params)
}
