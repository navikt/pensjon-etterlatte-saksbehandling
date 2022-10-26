
plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
    id("etterlatte.kafka")
}

dependencies {
    implementation(project(":libs:common"))

    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientJackson)
    implementation(Ktor2.ClientCioJvm)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientLogging)
    implementation(Ktor2.MetricsMicrometer)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.Auth)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.CallLogging)

    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor2)

    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(project(":libs:testdata"))
}