plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:etterlatte-helsesjekk"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))

    implementation("com.h2database:h2:2.1.214")

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.clientauth)

    implementation(libs.jackson.datatypejsr310)
    implementation(libs.jackson.modulekotlin)
    implementation(libs.database.kotliquery)

    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)
    testImplementation(libs.test.kotest.assertionscore)
}