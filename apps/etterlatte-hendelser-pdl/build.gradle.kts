plugins {
    alias(libs.plugins.avro)
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-pdl-model"))

    implementation(libs.ktor2.servercio)

    implementation(libs.kafka.avro) {
        exclude("org.apache.commons", "commons-compress")
    }
    implementation(libs.commons.compress)
    implementation(libs.kafka.avroserializer)

    testImplementation(libs.test.testcontainer.kafka)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
    testImplementation(testFixtures((project(":libs:etterlatte-kafka"))))
}

tasks.named("compileKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }
tasks.named("compileTestKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }
