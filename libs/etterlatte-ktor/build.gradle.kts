plugins {
    kotlin("jvm")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {

    implementation(libs.ktor.webjars)
    implementation(libs.ktor.serverresources)
    // Fram hit: ktor-avhengnadar for å dekkje det vi ekskluderer frå openapi-importen

    implementation(project(":libs:saksbehandling-common"))
    api(libs.ktor.auth)
    api(libs.ktor.servercorejvm)
    api(libs.ktor.servercio)
    api(libs.ktor.server)
    api(libs.ktor.clientcontentnegotiation)
    api(libs.ktor.jackson)
    api(libs.ktor.okhttp)
    api(libs.ktor.clientauth)
    api(libs.ktor.clientloggingjvm)

    api(libs.navfelles.tokenclientcore)

    implementation(libs.ktor.calllogging)
    implementation(libs.ktor.callid)
    implementation(libs.ktor.statuspages)
    implementation(libs.ktor.servercontentnegotiation)

    implementation(libs.ktor.metricsmicrometer)
    implementation(libs.ktor.doublereceive)

    api(libs.navfelles.tokenvalidationktor) {
        exclude("io.ktor", "ktor-server")
    }

    api(libs.kotlin.result)

    implementation(libs.logging.logstashlogbackencoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.metrics.micrometer.prometheus)
    implementation(libs.metrics.prometheus.simpleclientcommon)
    implementation(libs.metrics.prometheus.simpleclienthotspot)
    implementation(project(":libs:etterlatte-funksjonsbrytere"))

    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.engine)
    testRuntimeOnly(libs.test.junit.platform.launcher)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.mockk)
    testImplementation(libs.ktor.servertests)
    testImplementation(libs.navfelles.mockoauth2server)

    testFixturesImplementation(libs.navfelles.mockoauth2server)
    testFixturesImplementation(libs.ktor.servertests)
    testFixturesImplementation(libs.navfelles.tokenvalidationktor)
    testFixturesImplementation(libs.ktor.jackson)
    testFixturesImplementation(libs.ktor.servercontentnegotiation)
    testFixturesImplementation(libs.ktor.clientcontentnegotiation)
    testFixturesImplementation(project(":libs:saksbehandling-common"))
}
