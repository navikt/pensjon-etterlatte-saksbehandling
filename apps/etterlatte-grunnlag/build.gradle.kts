
plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:rapidsandrivers-extras"))
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
}
