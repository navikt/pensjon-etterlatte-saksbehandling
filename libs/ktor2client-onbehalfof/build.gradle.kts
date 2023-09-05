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

    api(libs.ktor2.clientcore)
    api(libs.ktor2.clientloggingjvm)
    api(libs.ktor2.okhttp)
    api(libs.ktor2.clientauth)
    api(libs.ktor2.clientcontentnegotiation)
    api(libs.ktor2.jackson)
    api(libs.ktor2.auth)
    api(libs.ktor2.authjwt)

    api("com.natpryce:konfig:1.6.10.0")
    api("com.michael-bull.kotlin-result:kotlin-result:1.1.18")
    api(libs.cache.caffeine)
    api(project(":libs:etterlatte-token-model"))

    implementation(libs.navfelles.tokenclientcore)

    testImplementation(libs.navfelles.tokenvalidationktor2)
    testImplementation(libs.kotlinx.coroutinestest)
    testImplementation(libs.test.wiremock)
    testImplementation(libs.test.jupiter.engine)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.ktor2.clientmock)
    testImplementation(libs.test.kotest.assertionscore)
    testImplementation(libs.test.mockk)

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
    }
}