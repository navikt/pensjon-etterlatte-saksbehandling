plugins {
    kotlin("jvm")
    id("etterlatte.libs")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.bundles.jackson)

    implementation(libs.etterlatte.common)
    implementation(libs.logging.logbackclassic)
    compileOnly(libs.logging.slf4japi)

    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.logging.slf4japi)
    testImplementation(libs.logging.logbackclassic)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.mockk)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
