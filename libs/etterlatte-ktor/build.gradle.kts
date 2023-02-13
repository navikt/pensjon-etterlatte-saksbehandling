
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:etterlatte-helsesjekk"))

    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.Auth)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.CallId)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ClientContentNegotiation)

    implementation(NavFelles.TokenValidationKtor2)

    implementation(Prometheus.SimpleClientCommon)
    implementation(Prometheus.SimpleClientHotspot)

    testImplementation(Jupiter.Engine)
    testImplementation(Ktor2.ServerTests)
    testImplementation(NavFelles.MockOauth2Server)

    tasks {
        withType<Test> {
            useJUnitPlatform()
        }
    }
}