plugins {
    id("etterlatte.kafka")
}

dependencies {
    api(kotlin("reflect"))

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
