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
    mavenCentral()
    maven("https://jitpack.io")
}
dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(libs.bundles.jackson)
    api(libs.navfelles.rapidandriversktor2)

    implementation(project(":libs:saksbehandling-common"))

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.mockk)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}