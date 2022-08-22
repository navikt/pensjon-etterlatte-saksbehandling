plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    // maven("https://kotlin.bintray.com/ktor")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(Ktor2.OkHttp)
    api(Ktor2.ClientCore)
    api(Ktor2.ClientAuth)
    api(Ktor2.ClientContentNegotiation)
    api(Ktor2.Jackson)
    api(Ktor2.ClientLoggingJvm)

    api(NavFelles.TokenClientCore)
}