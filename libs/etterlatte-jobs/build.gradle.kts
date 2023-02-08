plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Ktor2.ClientCore)
    implementation(Ktor2.ClientContentNegotiation)
    implementation(Ktor2.Jackson)

    compileOnly(Logging.Slf4jApi)

    testImplementation(Jupiter.Engine)
    testImplementation(Ktor2.ClientMock)
    testImplementation(MockK.MockK)
}
tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}