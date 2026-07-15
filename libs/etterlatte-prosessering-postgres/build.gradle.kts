plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:etterlatte-prosessering-core"))
    implementation(libs.database.hikaricp)
    implementation(libs.database.postgresql)
    implementation(libs.logging.slf4japi)

    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)
    testRuntimeOnly(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testRuntimeOnly(libs.logging.logbackclassic)
}
