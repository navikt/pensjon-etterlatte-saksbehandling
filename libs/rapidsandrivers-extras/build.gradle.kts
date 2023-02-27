plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.bundles.jackson)
    api(libs.navfelles.rapidandriversktor2)

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.mockk)
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