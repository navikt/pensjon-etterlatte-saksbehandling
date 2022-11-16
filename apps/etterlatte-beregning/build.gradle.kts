
plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {

    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.Auth)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.ClientAuth)
    implementation(NavFelles.TokenValidationKtor2)

    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.ModuleKotlin)

    implementation(project(":libs:common"))
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(project(":libs:testdata"))
}