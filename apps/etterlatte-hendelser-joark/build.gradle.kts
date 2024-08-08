plugins {
    alias(libs.plugins.avro)
    id("etterlatte.common")
}

repositories {
    maven("https://maven.pkg.github.com/navikt/teamdokumenthandtering-avro-schemas")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-oppgave-model"))

    implementation(libs.ktor2.servercio)

    implementation(libs.kafka.avro) {
        exclude("org.apache.commons", "commons-compress")
    }
    implementation(libs.commons.compress)

    implementation(libs.kafka.avroserializer)

    implementation(libs.teamdokumenthandtering.avroschemas)

    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.ktor2.clientmock)
}
