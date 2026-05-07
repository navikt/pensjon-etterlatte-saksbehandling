plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    runtimeOnly("com.papertrailapp:logback-syslog4j:1.0.0")

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(libs.test.kotest.assertionscore)
}
