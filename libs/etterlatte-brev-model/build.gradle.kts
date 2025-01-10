plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation("no.nav.pensjon.brevbaker:brevbaker-api-model-common:1.4.0")

    testImplementation(libs.test.jupiter.api)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)

    compileOnly(libs.logging.slf4japi)
}
