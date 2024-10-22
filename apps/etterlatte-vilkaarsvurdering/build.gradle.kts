plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))
    implementation(project(":libs:etterlatte-funksjonsbrytere"))

    implementation(libs.database.kotliquery)

    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(testFixtures((project(":libs:etterlatte-database"))))
    testImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testImplementation(testFixtures((project(":libs:saksbehandling-common"))))
}
