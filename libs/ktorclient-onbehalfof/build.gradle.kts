
plugins {
    kotlin("jvm")
    // id("com.github.johnrengelman.shadow")

}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    // maven("https://dl.bintray.com/michaelbull/maven")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    ktor("client-okhttp")
    ktor("client-core")
    ktor("client-logging-jvm")
    ktor("client-auth")
    ktor("client-jackson")
    ktor("auth")
    ktor("auth-jwt")


    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")

}

fun DependencyHandler.ktor(module: String){
    when(module){
        "client-jackson" -> api("io.ktor:ktor-$module:1.6.1")
        else -> api("io.ktor:ktor-$module:1.6.1") {
            exclude("org.jetbrains.kotlin:kotlin-reflect")
        }
    }
}