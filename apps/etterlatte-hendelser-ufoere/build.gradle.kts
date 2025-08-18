plugins {
    id("etterlatte.common")
}
repositories {
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-kafka"))

    implementation(libs.commons.compress)
    implementation(libs.kafka.avroserializer)

    testImplementation(libs.ktor.clientcontentnegotiation)
    testImplementation(libs.ktor.jackson)
    testImplementation(libs.test.testcontainer.kafka)
    testImplementation(libs.ktor.servertests)
    testImplementation(libs.ktor.clientmock)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
    testImplementation(testFixtures((project(":libs:etterlatte-kafka"))))
}
