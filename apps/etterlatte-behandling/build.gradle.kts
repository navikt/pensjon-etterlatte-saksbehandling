plugins {
    id("etterlatte.kafka")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-sporingslogg"))
    implementation(project(":libs:rapidsandrivers-extras"))
    implementation(project(":libs:etterlatte-jobs"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:etterlatte-pdl-model"))
    implementation(project(":libs:etterlatte-institusjonsopphold-model"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientciojvm)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientlogging)
    implementation(libs.ktor2.metricsmicrometer)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.calllogging)
    implementation(libs.kotlin.result)

    implementation(libs.metrics.micrometer.prometheus)
    implementation(libs.bundles.jackson)

    implementation(libs.bundles.navfelles.token)

    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(libs.test.mockk)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(project(":libs:testdata"))
    testImplementation(project(":libs:etterlatte-funksjonsbrytere"))
}