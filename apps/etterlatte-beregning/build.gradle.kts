plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-regler"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:ktor2client-onbehalfof"))

    implementation(Database.FlywayDB)
    implementation(Database.KotliQuery)

    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.ClientAuth)

    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.ModuleKotlin)

    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)

    testImplementation(MockK.MockK)
    testImplementation(Kotest.AssertionsCore)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(Ktor2.ServerTests)
    testImplementation(NavFelles.MockOauth2Server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(project(":libs:testdata"))
}