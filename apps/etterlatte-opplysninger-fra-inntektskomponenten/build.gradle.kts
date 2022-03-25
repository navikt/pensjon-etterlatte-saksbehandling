plugins {
    id("etterlatte.rapids-and-rivers")
}

dependencies {
    implementation(Ktor.OkHttp)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientLoggingJvm)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.ClientJackson)

    implementation(project(":libs:ktorclient-auth-clientcredentials"))
    implementation(project(":libs:common"))
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")

    testImplementation(Ktor.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
}