plugins {
    id("etterlatte.kafka")
}

dependencies {
    api(kotlin("reflect"))

    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.MetricsMicrometer)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.Auth)
    implementation(Ktor2.ServerHtmlBuilder)

    implementation(Micrometer.Prometheus)
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor2)

    implementation(Logging.LogbackClassic)
    implementation(Logging.LogstashLogbackEncoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(Jackson.Core)
    implementation(Jackson.Databind)


    testImplementation(MockK.MockK)
}
