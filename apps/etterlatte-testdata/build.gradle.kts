plugins {
    id("etterlatte.common")
    id("etterlatte.kafka")
}

dependencies {
    api(kotlin("reflect"))

    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-onbehalfof"))

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.metricsmicrometer)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.mustache)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.statuspages)

    implementation(libs.metrics.micrometer.prometheus)
    implementation(libs.bundles.jackson)
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)

    implementation(libs.navfelles.tokenclientcore)
    implementation(libs.navfelles.tokenvalidationktor2)

    implementation(libs.logging.logbackclassic)
    implementation(libs.logging.logstashlogbackencoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    testImplementation(libs.test.jupiter.root)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.kotest.assertionscore)
}