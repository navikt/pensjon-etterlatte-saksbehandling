
plugins {
    id("etterlatte.rapids-and-rivers")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)
    testImplementation(MockK.MockK)
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.flywaydb:flyway-core:6.5.0")
    implementation("org.postgresql:postgresql:42.2.5")
    testImplementation("org.testcontainers:junit-jupiter:1.15.3")
    testImplementation("org.testcontainers:postgresql:1.16.0")

}
