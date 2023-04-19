plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/pensjon-etterlatte-libs")
        credentials {
            username = "token"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-token-model"))

    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.clientcore)
    implementation(libs.database.flywaydb)

    testImplementation(libs.test.mockk)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(project(":libs:testdata"))
    testImplementation(libs.navfelles.mockoauth2server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)

    implementation(project(":apps:etterlatte-vilkaarsvurdering"))
    implementation(project(":apps:etterlatte-vilkaarsvurdering-kafka"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
    }
}