plugins {
    id("etterlatte.libs")
    id("java-library")
    id("java-test-fixtures")
}

repositories {
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(libs.bundles.jackson)
    implementation(project(":libs:saksbehandling-common"))
    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
    implementation(libs.kafka.clients)
    testImplementation(libs.kafka.avroserializer)

    testFixturesImplementation(libs.test.testcontainer.kafka)

    testFixturesImplementation(libs.ktor2.servercontentnegotiation)
    testFixturesImplementation(libs.kafka.clients)
}
