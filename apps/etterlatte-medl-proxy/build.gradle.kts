
plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(Ktor2.Auth)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientLoggingJvm)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientJackson)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)

    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:common"))

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor2)

    testImplementation(Ktor2.ClientMock)
    testImplementation(MockK.MockK)
}
