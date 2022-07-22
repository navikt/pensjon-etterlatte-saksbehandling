
plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:common"))
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
}
