plugins {
    id("etterlatte.common")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(Database.Postgresql)
    implementation(Database.FlywayDB)
    implementation(Database.HikariCP)

    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)
}