plugins {
    id("etterlatte.common")
    id("etterlatte.kafka")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/navikt/pensjon-etterlatte-libs")
        credentials {
            username = "token"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
}

dependencies {
    api(kotlin("reflect"))

    implementation(libs.logging.logbackclassic)
    implementation(libs.etterlatte.common)
    implementation(libs.logging.logstashlogbackencoder) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.jackson.core)
    implementation(libs.bundles.jackson)

    testImplementation(libs.test.mockk)
}