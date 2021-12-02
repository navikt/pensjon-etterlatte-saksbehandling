
plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:common"))
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.flywaydb:flyway-core:6.5.0")
    implementation("org.postgresql:postgresql:42.2.5")
    implementation(Ktor.ServerCore)
    implementation(Ktor.ServerCio)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientJackson)
    implementation(Ktor.ClientCioJvm)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.ClientLogging)
    implementation(Ktor.MetricsMicrometer)
    implementation(Ktor.Jackson)
    implementation(Ktor.Auth)

    implementation(Micrometer.Prometheus)
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor)

    testImplementation(MockK.MockK)
    testImplementation(Ktor.ClientMock)
    testImplementation(Ktor.ServerTests)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(NavFelles.MockOauth2Server)
    testImplementation("org.testcontainers:junit-jupiter:1.15.3")
    testImplementation("org.testcontainers:postgresql:1.16.0")
}
