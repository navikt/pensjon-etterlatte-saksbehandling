plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-jobs"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-ktor"))

    implementation(libs.ktor2.clientcore)

    implementation(libs.bundles.jackson)

    testImplementation(libs.test.mockk)
    testImplementation(libs.kotlinx.coroutinescore)
}