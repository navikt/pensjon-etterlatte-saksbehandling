plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    implementation(project(":libs:common"))
    implementation(libs.database.postgresql)
    implementation(libs.database.flywaydb)
    implementation(libs.database.hikaricp)
    implementation(libs.database.kotliquery)

    testImplementation(libs.test.jupiter.engine)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.testcontainer.jupiter)
    testImplementation(libs.test.testcontainer.postgresql)

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
    }
}