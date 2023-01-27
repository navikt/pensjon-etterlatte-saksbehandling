plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.common")
    id("etterlatte.kafka")
    id("com.faire.gradle.analyze") version "1.0.9"
}
dependencies {
    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.Jackson)

    implementation("org.jetbrains:annotations:24.0.0")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.3.1.0")
    implementation("org.messaginghub:pooled-jms:2.0.5")
    implementation(
        "com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:1.4201aa"
    )
    implementation("com.github.navikt.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:1.4201aa")

    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")

    implementation(project(":libs:common"))

    implementation(Database.HikariCP)
    implementation(Database.FlywayDB)
    implementation(Database.Postgresql)
    implementation(Database.KotliQuery)

    testImplementation(Ktor2.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)

    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("com.github.tomakehurst:wiremock:2.33.2")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation(TestContainer.Postgresql)
}