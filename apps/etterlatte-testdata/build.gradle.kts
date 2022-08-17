plugins {
    id("etterlatte.common")
    id("etterlatte.kafka")
}

dependencies {
    api(kotlin("reflect"))

    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-onbehalfof"))

    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.MetricsMicrometer)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.ClientJackson)
    implementation(Ktor2.Auth)
    implementation(Ktor2.Mustache)

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

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation(MockK.MockK)
    testImplementation(Kotest.AssertionsCore)
}
