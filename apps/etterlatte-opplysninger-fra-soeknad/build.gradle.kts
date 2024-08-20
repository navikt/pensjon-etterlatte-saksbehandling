plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:rapidsandrivers-extras"))
    implementation(libs.etterlatte.common)

    testImplementation(libs.ktor2.servertests)
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
