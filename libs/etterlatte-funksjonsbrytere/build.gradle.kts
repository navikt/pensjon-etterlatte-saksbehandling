plugins {
    id("etterlatte.libs")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    api(libs.unleash.client)
}
