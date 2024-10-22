plugins {
    kotlin("jvm")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    implementation(libs.openapi) {
        exclude("io.ktor", "ktor-server-core-jvm")
        exclude("io.ktor", "ktor-server-webjars")
        exclude("io.ktor", "ktor-server-auth")
        exclude("io.ktor", "ktor-server-resources")
    }
    implementation(libs.ktor2.webjars)
    implementation(libs.ktor2.serverresources)
    // Fram hit: ktor-avhengnadar for å dekkje det vi ekskluderer frå openapi-importen

    implementation(project(":libs:saksbehandling-common"))
    api(libs.ktor2.auth)
    api(libs.ktor2.servercorejvm)
    api(libs.ktor2.servercio)
    api(libs.ktor2.server)
    api(libs.ktor2.clientcontentnegotiation)
    api(libs.ktor2.jackson)
    api(libs.ktor2.okhttp)
    api(libs.ktor2.clientauth)
    api(libs.ktor2.clientloggingjvm)

    api(libs.navfelles.tokenclientcore)

    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.callid)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.servercontentnegotiation)

    implementation(libs.ktor2.metricsmicrometer)
    implementation(libs.ktor2.doublereceive)

    api(libs.navfelles.tokenvalidationktor2) {
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

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.mockk)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.navfelles.mockoauth2server)

    testFixturesImplementation(libs.navfelles.mockoauth2server)
    testFixturesImplementation(libs.ktor2.servertests)
    testFixturesImplementation(libs.navfelles.tokenvalidationktor2)
    testFixturesImplementation(libs.ktor2.jackson)
    testFixturesImplementation(libs.ktor2.servercontentnegotiation)
    testFixturesImplementation(libs.ktor2.clientcontentnegotiation)
    testFixturesImplementation(project(":libs:saksbehandling-common"))
}
