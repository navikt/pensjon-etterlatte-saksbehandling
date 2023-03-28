plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.bundles.jackson)

    implementation("no.nav.etterlatte:common:2022.09.27-14.09.182856a1fda5")

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