plugins {
    kotlin("jvm")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))

    testFixturesImplementation(testFixtures((project(":libs:etterlatte-ktor"))))
    testFixturesImplementation(testFixtures((project(":libs:saksbehandling-common"))))
    testFixturesImplementation(project(":libs:saksbehandling-common"))
    testFixturesImplementation(project(":libs:etterlatte-behandling-model"))
}
