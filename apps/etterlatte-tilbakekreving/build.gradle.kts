plugins {
    id("etterlatte.rapids-and-rivers")
    id("com.faire.gradle.analyze") version "1.0.9"
}
dependencies {
    implementation(project(":libs:ktorclient-auth-clientcredentials"))

    implementation(Ktor.ServerCore)
    implementation(Ktor.ServerCio)
    implementation(Ktor.ClientCioJvm)
    implementation(Ktor.OkHttp)
    implementation(Ktor.ClientCore)
    implementation(Ktor.ClientJackson)
    implementation(Ktor.ClientAuth)
    implementation(Ktor.Auth)
    implementation(Ktor.Jackson)
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)
    implementation(Jackson.Xml)

    implementation(NavFelles.TokenClientCore)
    implementation(NavFelles.TokenValidationKtor)

    implementation("org.jetbrains:annotations:13.0")

    implementation("com.ibm.mq:com.ibm.mq.allclient:9.2.5.0")
    implementation("org.messaginghub:pooled-jms:2.0.5")
    implementation("com.github.navikt.tjenestespesifikasjoner:tilbakekreving-v1-tjenestespesifikasjon:2589.e85bf84")


    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")

    implementation(project(":libs:common"))

    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.flywaydb:flyway-core:8.5.11")
    implementation("org.postgresql:postgresql:42.3.3")
    implementation("com.github.seratch:kotliquery:1.7.0")

    testImplementation(Ktor.ClientMock)
    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)

    testImplementation("org.testcontainers:testcontainers:1.16.3")
    testImplementation("com.github.tomakehurst:wiremock:2.33.2")
    testImplementation("org.testcontainers:junit-jupiter:1.16.3")
    testImplementation("org.testcontainers:postgresql:1.16.0")
}
