plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-helsesjekk"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))

    implementation(Database.HikariCP)
    implementation(Database.FlywayDB)
    implementation(Database.Postgresql)

    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.ClientLogging)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.Auth)
    implementation(Ktor2.AuthJwt)
    implementation(Cache.Caffeine)

    implementation(Micrometer.Prometheus)
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor2)

    testImplementation(MockK.MockK)
    testImplementation(Kotest.AssertionsCore)
    testImplementation(Ktor2.ClientMock)
    testImplementation(Ktor2.ServerTests)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(NavFelles.MockOauth2Server)
    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)
}