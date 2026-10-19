plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

tasks.jar.configure {
    dependsOn(":libs:etterlatte-mq:jar")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-tilbakekreving-model"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-mq"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(libs.mq.jakarta.client)
    // CVE-2026-8763: com.ibm.mq.jakarta.client pinner bcprov/bcpkix/bcutil til 1.84, som er sårbar. Tving opp til 1.85.
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.bouncycastle.bcutil)
    implementation(libs.navfelles.tjenestespesifikasjoner.tilbakekreving)

    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor.clientcontentnegotiation)
    testImplementation(libs.ktor.jackson)
    testImplementation(libs.ktor.clientmock)
    testImplementation(libs.ktor.servertests)
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures(project(":libs:etterlatte-mq")))
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
    testImplementation(libs.test.navfelles.rapidsandriversktor)
}
