plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:common"))

    testImplementation(libs.test.mockk)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(project(":libs:testdata"))
}