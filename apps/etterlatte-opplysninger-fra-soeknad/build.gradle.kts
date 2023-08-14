plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(libs.etterlatte.common)

    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(project(":libs:testdata"))
}