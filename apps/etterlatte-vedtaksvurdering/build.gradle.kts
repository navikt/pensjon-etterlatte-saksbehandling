plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-helsesjekk"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.metricsmicrometer)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.auth)

    implementation(libs.metrics.micrometer.prometheus)
    implementation(libs.bundles.jackson)

    implementation(libs.navfelles.tokenvalidationktor2)
    implementation(libs.database.kotliquery)

    testImplementation(libs.test.mockk)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(project(":libs:testdata"))
}