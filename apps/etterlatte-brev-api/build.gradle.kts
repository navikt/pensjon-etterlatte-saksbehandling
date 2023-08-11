plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-beregning-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))

    implementation("no.nav.pensjon.brevbaker:brevbaker-api-model-common:1.0.2")

    implementation(libs.database.hikaricp)
    implementation(libs.database.flywaydb)
    implementation(libs.database.postgresql)
    implementation(libs.database.kotliquery)

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientlogging)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.authjwt)
    implementation(libs.cache.caffeine)

    implementation(libs.bundles.jackson)

    implementation(libs.navfelles.tokenclientcore)
    implementation(libs.navfelles.tokenvalidationktor2)

    testImplementation(libs.test.mockk)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)
    testImplementation(project(":libs:testdata"))
}