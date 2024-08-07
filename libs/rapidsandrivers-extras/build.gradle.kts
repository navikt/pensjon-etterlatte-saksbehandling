plugins {
    kotlin("jvm")
    id("etterlatte.libs")
}

dependencies {
    api(libs.bundles.jackson)
    api(libs.navfelles.rapidandriversktor2)

    implementation(project(":libs:saksbehandling-common"))

    compileOnly(libs.logging.slf4japi)

    testImplementation(libs.test.mockk)
    testImplementation(libs.test.jupiter.api)
    testImplementation(libs.test.jupiter.params)
    testRuntimeOnly(libs.test.jupiter.engine)
    testImplementation(libs.test.kotest.assertionscore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}
