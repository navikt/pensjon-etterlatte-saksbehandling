plugins {
    alias(libs.plugins.avro)
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(libs.kafka.clients)
    implementation(libs.kafka.avroserializer)
}