plugins {
    kotlin("jvm")
}

repositories {
    maven("https://jitpack.io")
    mavenCentral()
}

dependencies {
    implementation(project(":libs:common"))

    testImplementation(Jupiter.Engine)
}