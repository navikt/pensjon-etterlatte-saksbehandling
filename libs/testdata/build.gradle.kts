plugins {
    kotlin("jvm")
}

repositories {
    maven("https://github.com")
    mavenCentral()
}

dependencies {
    implementation(project(":libs:common"))

    testImplementation(Jupiter.Engine)
}