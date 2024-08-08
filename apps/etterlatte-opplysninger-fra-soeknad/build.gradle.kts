plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(libs.etterlatte.common)

    testImplementation(libs.ktor2.servertests)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
