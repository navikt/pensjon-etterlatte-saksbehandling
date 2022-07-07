plugins {
    application
    kotlin("jvm")
    id("com.commercehub.gradle.plugin.avro") version "0.21.0"
}

repositories {
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(Ktor.ServerCore)
    implementation(Ktor.ServerNetty)
    implementation(Ktor.MetricsMicrometer)
    implementation(Ktor.Jackson)
    implementation(Micrometer.Prometheus)
    implementation(Jackson.Databind)
    implementation(Jackson.ModuleKotlin)
    implementation(Jackson.DatatypeJsr310)
    implementation(Kafka.Clients)
    implementation(Kafka.Avro)
    implementation(Kafka.AvroSerializer) {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
    implementation(NavFelles.RapidAndRivers)
    implementation(Logging.LogbackClassic)

    testImplementation(Jupiter.Api)
    testImplementation(Kafka.EmbeddedEnv) {
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
    }
    testImplementation(Jupiter.Engine)
    testImplementation(Ktor.ServerTests)

}

tasks.named<Jar>("jar") {
    archiveBaseName.set("app")

    manifest {
        attributes["Main-Class"] = "no.nav.etterlatte.ApplicationKt"
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }

    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}