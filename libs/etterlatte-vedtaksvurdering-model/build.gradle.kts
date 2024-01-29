plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
}
