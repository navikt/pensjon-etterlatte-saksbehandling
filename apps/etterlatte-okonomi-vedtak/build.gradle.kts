plugins {
    id("etterlatte.rapids-and-rivers")
    id("com.faire.gradle.analyze") version "1.0.9"
}

dependencies {
    implementation(Ktor.OkHttp)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientJackson)

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.2.5.0")
    implementation("org.messaginghub:pooled-jms:2.0.5")
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:1.4201aa")
    implementation("com.github.navikt.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:1.4201aa")

    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")

    implementation(project(":libs:common"))

    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.flywaydb:flyway-core:6.5.0")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("com.github.seratch:kotliquery:1.7.0")

    testImplementation(Ktor.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)

    testImplementation("org.testcontainers:testcontainers:1.16.3")
    testImplementation("com.github.tomakehurst:wiremock:2.27.2")
    testImplementation("org.testcontainers:junit-jupiter:1.16.3")
    testImplementation("org.testcontainers:postgresql:1.16.0")
}
