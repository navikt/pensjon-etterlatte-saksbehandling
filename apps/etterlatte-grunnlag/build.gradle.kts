plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:etterlatte-sporingslogg"))

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientciojvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientlogging)
    implementation(libs.ktor2.metricsmicrometer)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.calllogging)

    implementation(libs.bundles.jackson)

    implementation(libs.navfelles.tokenclientcore)
    implementation(libs.navfelles.tokenvalidationktor2)

    testImplementation(libs.test.mockk)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.ktor2.servertests)
    testImplementation(project(":libs:testdata"))
}