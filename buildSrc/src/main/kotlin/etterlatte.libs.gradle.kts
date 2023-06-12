plugins {
    kotlin("jvm")
    application
    id("org.cyclonedx.bom") version "1.7.4"
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