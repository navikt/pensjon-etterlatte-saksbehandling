plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("com.faire.gradle.analyze") version "1.0.9"
}
dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))

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

    implementation("com.ibm.mq:com.ibm.mq.jakarta.client:9.3.1.0")
    implementation("org.messaginghub:pooled-jms:3.1.0")
    implementation("com.github.navikt.tjenestespesifikasjoner:tilbakekreving-v1-tjenestespesifikasjon:2589.e85bf84")

    implementation(JakartaBind.api)
    implementation(JakartaBind.impl)

    implementation(Database.HikariCP)
    implementation(Database.FlywayDB)
    implementation(Database.Postgresql)
    implementation(Database.KotliQuery)

    testImplementation(Ktor2.ClientMock)
    testImplementation(Ktor2.ServerTests)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
    testImplementation(NavFelles.MockOauth2Server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(WireMock.WireMock)
    testImplementation(TestContainer.Jupiter)
    testImplementation(TestContainer.Postgresql)
}