plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:etterlatte-helsesjekk"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))

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

    testImplementation(libs.test.mockk)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(project(":libs:testdata"))
}

tasks {
    test {
        testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}