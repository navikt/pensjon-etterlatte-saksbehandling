plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:ktor2client-onbehalfof"))
    implementation(project(":libs:etterlatte-ktor"))

    implementation(Ktor2.OkHttp)
    implementation(Ktor2.ServerCore)
    implementation(Ktor2.ServerCio)
    implementation(Ktor2.Jackson)
    implementation(Ktor2.CallLogging)
    implementation(Ktor2.StatusPages)
    implementation(Ktor2.ServerContentNegotiation)
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.ClientAuth)

    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.ModuleKotlin)
    implementation(Database.KotliQuery)

    testImplementation(MockK.MockK)
    testImplementation(Ktor2.ServerTests)
    testImplementation(Kotest.AssertionsCore)
    testImplementation(NavFelles.MockOauth2Server) {
        exclude("org.slf4j", "slf4j-api")
    }
    testImplementation(project(":libs:testdata"))
}