plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:common"))

    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientLoggingJvm)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientJackson)
    implementation(Ktor2.ClientContentNegotiation)

    testImplementation(Jupiter.Root)
    testImplementation(Ktor2.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(project(":libs:testdata"))
}