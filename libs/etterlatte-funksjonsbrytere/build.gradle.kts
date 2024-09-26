plugins {
    kotlin("jvm")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    api(libs.unleash.client)
}
