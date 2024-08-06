plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.common")
    id("etterlatte.postgres")
}

tasks.jar.configure {
    dependsOn(":libs:etterlatte-mq:jar")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-jobs"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-utbetaling-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-kafka"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-mq"))
    implementation(project(":libs:etterlatte-migrering-model"))

    implementation(libs.mq.jakarta.client)
    implementation(libs.navfelles.tjenestespesifikasjoner.oppdragsbehandling)
    implementation(libs.navfelles.tjenestespesifikasjoner.oppdragsimulering)
    implementation(libs.navfelles.tjenestespesifikasjoner.avstemming)

    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures((project(":libs:etterlatte-funksjonsbrytere"))))
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(testFixtures(project(":libs:etterlatte-mq")))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
