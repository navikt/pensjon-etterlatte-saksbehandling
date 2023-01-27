plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("com.faire.gradle.analyze") version "1.0.9"
}
dependencies {
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

    implementation("org.jetbrains:annotations:13.0")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.2.5.0")
    implementation("org.messaginghub:pooled-jms:2.0.5")
    implementation("com.github.navikt.tjenestespesifikasjoner:tilbakekreving-v1-tjenestespesifikasjon:2589.e85bf84")

    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")

    implementation(project(":libs:common"))

    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation(Database.FlywayDB)
    implementation(Database.Postgresql)
    implementation(Database.KotliQuery)

    testImplementation(Ktor2.ClientMock)
    testImplementation(Ktor2.ServerTests)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)

    testImplementation("org.testcontainers:testcontainers:1.16.3")
    testImplementation("com.github.tomakehurst:wiremock:2.33.2")
    testImplementation("org.testcontainers:junit-jupiter:1.16.3")
    testImplementation("org.testcontainers:postgresql:1.16.0")
}