plugins {
    id("etterlatte.common")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-beregning-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-sporingslogg"))

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientciojvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientlogging)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.metricsmicrometer)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.auth)

    implementation(libs.bundles.jackson)

    implementation(libs.navfelles.tokenvalidationktor2)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.kotest.assertionscore)
}
