plugins {
    id("com.github.davidmc24.gradle.plugin.avro") version "1.3.0"
    id("etterlatte.rapids-and-rivers")
    id("etterlatte.kafka")
}

dependencies {
    implementation(Ktor.OkHttp)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientLoggingJvm)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.ClientJackson)
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
    testImplementation(MockK.MockK)
    testImplementation(Ktor.ClientMock)
    implementation(project(":libs:ktorclient-auth-clientcredentials"))
    implementation(project(":libs:common"))

}

tasks.named("compileKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }
