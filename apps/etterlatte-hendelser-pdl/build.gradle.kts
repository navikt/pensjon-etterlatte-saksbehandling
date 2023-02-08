plugins {
    id("com.github.davidmc24.gradle.plugin.avro") version "1.6.0"
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.kafka")
}

dependencies {
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:common"))

    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientLoggingJvm)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientJackson)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.Jackson)
    implementation(Jackson.Databind)
    implementation(Jackson.ModuleKotlin)
    implementation(Jackson.DatatypeJsr310)
    implementation(Kafka.Clients)
    implementation(Kafka.Avro)
    implementation(Kafka.AvroSerializer)
    implementation(Logging.LogbackClassic)

    testImplementation(Jupiter.Api)
    testImplementation(Kafka.EmbeddedEnv)
    testImplementation(Jupiter.Engine)
    testImplementation(Ktor2.ServerTests)
    testImplementation(MockK.MockK)
    testImplementation(Ktor2.ClientMock)
}

tasks.named("compileKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }
tasks.named("compileTestKotlin").configure { dependsOn(":apps:etterlatte-hendelser-pdl:generateAvroJava") }