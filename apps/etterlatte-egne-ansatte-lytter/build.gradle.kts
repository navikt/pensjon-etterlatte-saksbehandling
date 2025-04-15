plugins {
    alias(libs.plugins.avro)
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(libs.kafka.avro) {
        exclude("org.apache.commons", "commons-compress")
    }
    implementation(libs.commons.compress)

    testImplementation(libs.test.testcontainer.kafka)
    testImplementation(libs.ktor2.servertests)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(testFixtures((project(":libs:etterlatte-kafka"))))
}
