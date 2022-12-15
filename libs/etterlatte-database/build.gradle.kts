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

    implementation(Database.Postgresql)
    implementation(Database.FlywayDB)
    implementation(Database.HikariCP)

    testImplementation(Jupiter.Engine)
    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
    }
}