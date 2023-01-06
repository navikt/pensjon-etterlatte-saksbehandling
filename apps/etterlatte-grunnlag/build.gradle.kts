
plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:ktor2client-onbehalfof"))

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
    testImplementation(NavFelles.MockOauth2Server)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(Ktor2.ServerTests)
    testImplementation(project(":libs:testdata"))
}