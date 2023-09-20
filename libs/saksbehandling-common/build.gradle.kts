plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.bundles.jackson)

    implementation(libs.etterlatte.common)

    compileOnly(libs.logging.slf4japi)
    testImplementation(libs.logging.slf4japi)
    testImplementation(libs.logging.logbackclassic)

    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
