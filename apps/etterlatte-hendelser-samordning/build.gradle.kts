plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))

    implementation(libs.kafka.clients)
    implementation(libs.kafka.avro) {
        exclude("org.apache.commons", "commons-compress")
    }
    implementation(libs.commons.compress)

    testImplementation(libs.test.testcontainer.kafka)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.kotest.assertionscore)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(testFixtures((project(":libs:etterlatte-kafka"))))
}
