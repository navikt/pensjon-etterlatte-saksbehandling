plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}
dependencies {
    implementation(project(":libs:common"))

    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.Auth)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ServerContentNegotiation)

    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.ModuleKotlin)

    implementation(NavFelles.TokenValidationKtor2)

    implementation(Database.HikariCP)
    implementation(Database.FlywayDB)
    implementation(Database.Postgresql)
    implementation(Database.KotliQuery)

    testImplementation(MockK.MockK)
    testImplementation(Ktor2.ServerTests)
    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)
    testImplementation(Kotest.AssertionsCore)
    testImplementation(NavFelles.MockOauth2Server)
    testImplementation(project(":libs:testdata"))
}