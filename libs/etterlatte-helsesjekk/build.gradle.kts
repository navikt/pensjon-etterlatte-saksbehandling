plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(libs.ktor2.okhttp)
}