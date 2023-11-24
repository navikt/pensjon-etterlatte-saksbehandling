plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(mapOf("path" to ":libs:etterlatte-behandling-model")))
    implementation(project(mapOf("path" to ":libs:etterlatte-behandling-model")))
    implementation(project(mapOf("path" to ":libs:etterlatte-behandling-model")))
}
