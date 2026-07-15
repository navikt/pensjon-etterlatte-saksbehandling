plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:etterlatte-prosessering-core"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.servercorejvm)
    implementation(libs.logging.slf4japi)

    testImplementation(project(":libs:etterlatte-prosessering-postgres"))
    testImplementation(libs.database.hikaricp)
    testImplementation(libs.database.postgresql)
    testImplementation(libs.ktor.servertests)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)
    testRuntimeOnly(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testRuntimeOnly(libs.logging.logbackclassic)
}
