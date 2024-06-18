plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-regler"))

    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
