plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.jackson)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.modulekotlin)
    implementation(libs.jackson.datatypejsr310)

    implementation(libs.kafka.clients)
    implementation(libs.kafka.avro) {
        exclude("org.apache.commons", "commons-compress")
    }
    implementation(libs.commons.compress)

    testImplementation(libs.test.testcontainer.kafka)
    testImplementation(libs.el.impl)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:etterlatte-kafka"))))
}
