plugins {
    id("etterlatte.kafka")
}

dependencies {
    api(kotlin("reflect"))

    implementation(Ktor.ServerCore)
    implementation(Ktor.ServerCio)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientJackson)
    implementation(Ktor.ClientCioJvm)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.ClientLogging)
    implementation(Ktor.MetricsMicrometer)
    implementation(Ktor.Jackson)
    implementation(Ktor.Auth)

    implementation(Micrometer.Prometheus)
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor)

    implementation(Logging.LogbackClassic)
    implementation(Logging.LogstashLogbackEncoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(Jackson.Core)
    implementation(Jackson.Databind)
    implementation(Jackson.ModuleKotlin)
    implementation(Jackson.DatatypeJsr310)

    testImplementation(MockK.MockK)
}
