
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

    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientLoggingJvm)
    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.Auth)
    implementation(Ktor2.AuthJwt)


    implementation("com.natpryce:konfig:1.6.10.0")
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")

}
