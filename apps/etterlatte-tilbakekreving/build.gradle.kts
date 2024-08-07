plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

tasks.jar.configure {
    dependsOn(":libs:etterlatte-mq:jar")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-tilbakekreving-model"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-mq"))

    implementation(libs.mq.jakarta.client)
    implementation(libs.navfelles.tjenestespesifikasjoner.tilbakekreving)

    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor2.clientcontentnegotiation)
    testImplementation(libs.ktor2.jackson)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures(project(":libs:etterlatte-mq")))
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
}
