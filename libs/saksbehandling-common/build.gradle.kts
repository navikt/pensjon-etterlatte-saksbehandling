
plugins {
    kotlin("jvm")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.bundles.jackson)

    implementation(libs.etterlatte.common)
    implementation(libs.logging.logstashlogbackencoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }
    testFixturesImplementation(libs.etterlatte.common)
    implementation(libs.logging.logbackclassic)
    compileOnly(libs.logging.slf4japi)

    testRuntimeOnly(libs.test.jupiter.engine)
    testFixturesRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(libs.logging.slf4japi)
    testImplementation(libs.logging.logbackclassic)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.mockk)
}
