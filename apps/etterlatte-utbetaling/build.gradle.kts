@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.common")
    id("etterlatte.kafka")
    alias(libs.plugins.analyze)
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-jobs"))
    implementation(project(":libs:etterlatte-database"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.jackson)

    implementation(libs.mq.jakarta.client)
    implementation(libs.messaginghub.pooled.jms)
    implementation(libs.navfelles.tjenestespesifikasjoner.oppdragsbehandling)
    implementation(libs.navfelles.tjenestespesifikasjoner.avstemming)

    implementation(libs.jakartabind.api)
    implementation(libs.jakartabind.impl)

    implementation(libs.database.hikaricp)
    implementation(libs.database.flywaydb)
    implementation(libs.database.postgresql)
    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.mockk)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.test.wiremock)
    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)
}