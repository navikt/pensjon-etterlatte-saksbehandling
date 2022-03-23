plugins {
    id("etterlatte.rapids-and-rivers")
}

dependencies {
    implementation(Ktor.OkHttp)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientLoggingJvm)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.ClientJackson)

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.2.5.0")
    implementation("com.github.navikt.tjenestespesifikasjoner:nav-virksomhet-oppdragsbehandling-v1-meldingsdefinisjon:1.4201aa")

    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")

    implementation(project(":libs:ktorclient-auth-clientcredentials"))
    implementation(project(":libs:common"))

    testImplementation(Ktor.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)

    testImplementation("org.testcontainers:testcontainers:1.16.3")
    testImplementation("org.testcontainers:junit-jupiter:1.16.3")
}
