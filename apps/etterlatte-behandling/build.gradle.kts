plugins {
    id("etterlatte.kafka")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-sporingslogg"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientJackson)
    implementation(Ktor2.ClientCioJvm)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientLogging)
    implementation(Ktor2.MetricsMicrometer)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.Auth)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.CallLogging)
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")

    implementation(Micrometer.Prometheus)
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor2)

    testImplementation(Ktor2.ClientContentNegotiation)
    testImplementation(MockK.MockK)
    testImplementation(Ktor2.ClientMock)
    testImplementation(Ktor2.ServerTests)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(NavFelles.MockOauth2Server)
    testImplementation(project(":libs:testdata"))
}