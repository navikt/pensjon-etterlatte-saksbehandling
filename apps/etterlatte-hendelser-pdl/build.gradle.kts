plugins {
    id("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
    id("etterlatte.rapids-and-rivers")
    id("etterlatte.kafka")
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
    implementation(Kafka.AvroSerializer)
    implementation(NavFelles.RapidAndRivers)
    implementation(Logging.LogbackClassic)
    testImplementation(Jupiter.Api)
    testImplementation(Kafka.EmbeddedEnv)
    testImplementation(Jupiter.Engine)
    testImplementation(Ktor.ServerTests)

}