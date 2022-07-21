
plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(NavFelles.TokenValidationKtor2)
    implementation(Ktor2.ServerHtmlBuilder)

    testImplementation(Ktor2.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
}
