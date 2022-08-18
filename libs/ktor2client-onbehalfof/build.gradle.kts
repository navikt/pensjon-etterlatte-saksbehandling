
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(Ktor2.ClientCore)
    api(Ktor2.ClientLoggingJvm)
    api(Ktor2.OkHttp)
    api(Ktor2.ClientAuth)
    api(Ktor2.ClientContentNegotiation)
    api(Ktor2.Jackson)
    api(Ktor2.Auth)
    api(Ktor2.AuthJwt)

    api("com.natpryce:konfig:1.6.10.0")
    api("com.michael-bull.kotlin-result:kotlin-result:1.1.14")
    api("com.github.ben-manes.caffeine:caffeine:3.1.1")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.33.2")
    testImplementation(Jupiter.Engine)
    testImplementation(Jupiter.Api)
    testImplementation(Ktor2.ClientMock)
    testImplementation(Kotest.AssertionsCore)

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
    }
}