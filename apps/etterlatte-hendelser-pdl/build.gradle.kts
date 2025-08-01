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
    implementation(project(":libs:etterlatte-pdl-model"))

    implementation(libs.kafka.avro) {
        exclude("org.apache.commons", "commons-compress")
    }
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

tasks.named("compileKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }
tasks.named("compileTestKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }
