plugins {
    kotlin("jvm")
}

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/navikt/pensjon-etterlatte-libs")
        credentials {
            username = "token"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven("https://github.com")
    mavenCentral()
}

dependencies {
    implementation(project(":libs:common"))
    implementation(Etterlatte.Common)

    testImplementation(libs.test.jupiter.engine)
}