plugins {
    id("etterlatte.libs")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(libs.database.postgresql)
    implementation(libs.database.flywaydb)
    implementation(libs.database.flywaydbpostgres)
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
    testFixturesImplementation(libs.database.hikaricp)
    testFixturesImplementation(libs.database.kotliquery)
}
