plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

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

    implementation(libs.mq.jakarta.client) {
        exclude("org.bouncycastle:bcutil-jdk18on")
        exclude("org.bouncycastle:bcpkix-jdk18on")
        exclude("org.bouncycastle:bcprov-jdk18on")
    }
    implementation(libs.messaginghub.pooled.jms)
    implementation(libs.navfelles.tjenestespesifikasjoner.tilbakekreving)

    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(project(":libs:testdata"))
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures(project(":libs:etterlatte-mq")))
    evaluationDependsOn(":libs:etterlatte-mq")
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))

    // Avhengigheter fra patching av sårbarheter i IBM MQ.
    // Vi bør kunne ta bort alle disse og exclude-lista for neste IBM MQ-versjon
    implementation(libs.bcpkix)
    implementation(libs.bcprov)
    implementation(libs.bcutil)
}
