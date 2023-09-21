plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
    alias(libs.plugins.analyze)
}

val cxfVersion = "4.0.3"

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-tilbakekreving-model"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-mq"))

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.clientciojvm)
    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.bundles.jackson)
    implementation(libs.jackson.xml)

    implementation(libs.navfelles.tokenclientcore)
    implementation(libs.navfelles.tokenvalidationktor2)

    implementation(libs.mq.jakarta.client)
    implementation(libs.messaginghub.pooled.jms)
    implementation(libs.navfelles.tjenestespesifikasjoner.tilbakekreving)
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion") {
        // https://security.snyk.io/vuln/SNYK-JAVA-ORGAPACHEVELOCITY-3116414
        exclude(group = "org.apache.velocity")
    }

    implementation(libs.jakartabind.api)
    implementation(libs.jakartabind.impl)

    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(libs.test.wiremock)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(project(":libs:testdata"))
    testImplementation(testFixtures(project(":libs:etterlatte-mq")))
}
