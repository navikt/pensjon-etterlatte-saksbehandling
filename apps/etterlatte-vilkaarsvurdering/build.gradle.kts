plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
}
dependencies {
    implementation(project(":libs:common"))

    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.Auth)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.ClientAuth)

    implementation(project(":libs:ktor2client-onbehalfof"))

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