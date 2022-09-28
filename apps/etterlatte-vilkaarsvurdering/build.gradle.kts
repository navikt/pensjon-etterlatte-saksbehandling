plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("com.faire.gradle.analyze") version "1.0.9"
}
dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))

    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.ClientCioJvm)
    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientJackson)
    implementation(Ktor2.ClientAuth)
    implementation(Ktor2.Auth)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.ServerContentNegotiation)

    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)
    implementation(Jackson.Xml)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor2)

    implementation(Database.HikariCP)
    implementation(Database.FlywayDB)
    implementation(Database.Postgresql)
    implementation(Database.KotliQuery)

    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(Ktor2.ClientMock)
    testImplementation(Ktor2.ServerTests)

    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)
}