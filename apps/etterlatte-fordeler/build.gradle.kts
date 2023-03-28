plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(Etterlatte.Common)

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientloggingjvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.jackson)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.mockk)
    testImplementation(libs.kotlinx.coroutinescore)
}