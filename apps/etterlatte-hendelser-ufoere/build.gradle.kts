plugins {
    alias(libs.plugins.avro)
    id("etterlatte.common")
}
repositories {
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-kafka"))

    implementation(libs.kafka.avro) {
        exclude("org.apache.commons", "commons-compress")
    }
    implementation(libs.commons.compress)
    implementation(libs.kafka.avroserializer)

    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(libs.ktor2.jackson)
    testImplementation(libs.test.testcontainer.kafka)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
    testImplementation(testFixtures((project(":libs:etterlatte-kafka"))))
}
