plugins {
    id("etterlatte.rapids-and-rivers")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:ktorclient-onbehalfof"))
    implementation(project(":libs:rapidsandrivers-extras"))

    implementation(Database.HikariCP)
    implementation(Database.FlywayDB)
    implementation(Database.Postgresql)

    implementation(Ktor.ServerCore)
    implementation(Ktor.ServerCio)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientJackson)
    implementation(Ktor.ClientCioJvm)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.ClientLogging)
    implementation(Ktor.Jackson)
    implementation(Ktor.Auth)
    implementation(project(":libs:ktorclient-auth-clientcredentials"))
    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")

    implementation(Micrometer.Prometheus)
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor)

    testImplementation(MockK.MockK)
    testImplementation(Kotest.AssertionsCore)
    testImplementation(Ktor.ClientMock)
    testImplementation(Ktor.ServerTests)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(NavFelles.MockOauth2Server)
    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)
}
