plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:rapidsandrivers-extras"))
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
