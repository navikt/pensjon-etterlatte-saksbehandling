plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-regler"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-beregning-model"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-jobs"))
    implementation(project(":libs:etterlatte-trygdetid-model"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))

    implementation(libs.database.kotliquery)

    implementation(libs.ktor2.servercio)
    implementation(libs.navfelles.tokenclientcore)

    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.kotlinx.coroutinescore)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures((project(":libs:etterlatte-funksjonsbrytere"))))
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
