plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers")
}

dependencies {
    implementation(project(":libs:common"))
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.flywaydb:flyway-core:6.5.0")
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
}
