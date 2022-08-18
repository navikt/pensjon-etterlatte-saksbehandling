
plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:common"))
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
}