plugins {
    kotlin("jvm")
    id("etterlatte.libs")
    id("java-library")
    id("java-test-fixtures")
}

repositories {
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    implementation(project(":libs:saksbehandling-common"))
    implementation(libs.database.postgresql)
    implementation(libs.database.flywaydb)
    implementation(libs.database.hikaricp)
    implementation(libs.database.kotliquery)

    testImplementation(libs.test.jupiter.engine)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)

    testFixturesImplementation(libs.test.jupiter.engine)
    testFixturesImplementation(libs.test.mockk)
    testFixturesImplementation(libs.test.testcontainer.jupiter)
    testFixturesImplementation(libs.test.testcontainer.postgresql)
    testFixturesImplementation(libs.database.flywaydb)

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
    }
}