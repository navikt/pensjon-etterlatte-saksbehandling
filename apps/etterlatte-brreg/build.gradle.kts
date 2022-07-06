
plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientLoggingJvm)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.ClientJackson)
    implementation(Ktor.Jackson)
    implementation(Ktor.OkHttp)
    implementation(Ktor.ServerCore)
    implementation(Ktor.ServerCio)

    implementation(project(":libs:ktorclient-auth-clientcredentials"))
    implementation(project(":libs:common"))

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor)

    testImplementation(Ktor.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
}
