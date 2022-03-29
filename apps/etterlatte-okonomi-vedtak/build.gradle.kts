plugins {
    id("etterlatte.rapids-and-rivers")
}

dependencies {
    implementation(Ktor.OkHttp)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientLoggingJvm)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.ClientJackson)

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.2.0.1")
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:1.4201aa")

    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")

    implementation(project(":libs:ktorclient-auth-clientcredentials"))
    implementation(project(":libs:common"))

    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.flywaydb:flyway-core:6.5.0")
    implementation("org.postgresql:postgresql:42.2.5")

    testImplementation(Ktor.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)

    testImplementation("org.testcontainers:testcontainers:1.16.3")
    testImplementation("org.testcontainers:junit-jupiter:1.16.3")
    testImplementation("org.testcontainers:postgresql:1.16.0")
}
