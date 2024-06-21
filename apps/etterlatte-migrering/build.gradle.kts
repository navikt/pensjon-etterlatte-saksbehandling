plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-migrering-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(project(":libs:etterlatte-pdl-model"))
    implementation(project(":libs:etterlatte-utbetaling-model"))
    implementation(project(":libs:etterlatte-brev-model"))

    implementation(libs.ktor2.servercio)
    implementation(libs.database.kotliquery)
    testImplementation(libs.ktor2.servertests)
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
}
