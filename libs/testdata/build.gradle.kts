plugins {
    kotlin("jvm")
    id("etterlatte.rapids-and-rivers-ktor2")
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":libs:common"))
}