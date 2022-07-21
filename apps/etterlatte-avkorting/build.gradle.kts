
plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:rapidsandrivers-extras"))
    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
}
