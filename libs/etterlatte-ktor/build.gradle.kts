plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

repositories {
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:etterlatte-token-model"))

    implementation(libs.ktor2.servercore)
    implementation(libs.ktor2.servercio)
    implementation(libs.ktor2.auth)
    implementation(libs.ktor2.jackson)
    implementation(libs.ktor2.calllogging)
    implementation(libs.ktor2.callid)
    implementation(libs.ktor2.statuspages)
    implementation(libs.ktor2.servercontentnegotiation)
    implementation(libs.ktor2.okhttp)
    implementation(libs.ktor2.clientcontentnegotiation)
    implementation(libs.ktor2.metricsmicrometer)

    implementation(libs.navfelles.tokenvalidationktor2)

    implementation(libs.logging.logstashlogbackencoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.metrics.micrometer.prometheus)
    implementation(libs.metrics.prometheus.simpleclientcommon)
    implementation(libs.metrics.prometheus.simpleclienthotspot)
    implementation(project(":libs:etterlatte-funksjonsbrytere"))

    testImplementation(libs.test.jupiter.engine)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.mockk)

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
    }
}
