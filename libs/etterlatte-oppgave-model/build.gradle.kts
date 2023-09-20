plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(project(":libs:testdata"))
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
