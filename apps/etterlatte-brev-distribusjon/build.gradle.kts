
plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientLoggingJvm)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientJackson)
    implementation(Ktor2.ClientContentNegotiation)

    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:common"))
    implementation("org.junit.jupiter:junit-jupiter:5.9.2")

    testImplementation(Ktor2.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotest.AssertionsCore)
    testImplementation(Kotlinx.CoroutinesCore)
}