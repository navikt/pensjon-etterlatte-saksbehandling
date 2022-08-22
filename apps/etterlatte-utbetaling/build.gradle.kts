plugins {
    id("etterlatte.rapids-and-rivers-ktor2")
    id("com.faire.gradle.analyze") version "1.0.9"
}

dependencies {
    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.Jackson)

    implementation("org.jetbrains:annotations:13.0")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.2.5.0")
    implementation("org.messaginghub:pooled-jms:2.0.5")
    implementation(
        "com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:1.4201aa"
    )
    implementation("com.github.navikt.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:1.4201aa")

    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")

    implementation(project(":libs:common"))

    implementation(Database.HikariCP)
    implementation("org.flywaydb:flyway-core:8.5.11")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("com.github.seratch:kotliquery:1.7.0")

    testImplementation(Ktor2.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)

    testImplementation("org.testcontainers:testcontainers:1.16.3")
    testImplementation("com.github.tomakehurst:wiremock:2.33.2")
    testImplementation("org.testcontainers:junit-jupiter:1.16.3")
    testImplementation(TestContainer.Postgresql)
}