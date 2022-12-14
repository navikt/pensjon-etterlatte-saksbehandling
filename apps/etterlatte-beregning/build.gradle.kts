
plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:etterlatte-ktor"))

    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(NavFelles.TokenValidationKtor2)

    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:common"))
    implementation(Database.Postgresql)
    implementation(Database.HikariCP)
    implementation(Database.FlywayDB)
    implementation(Database.KotliQuery)

    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)

    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(project(":libs:testdata"))
}