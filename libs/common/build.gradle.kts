import Logging.Slf4jApi

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(Jackson.DatatypeJsr310)
    api(Jackson.DatatypeJdk8)
    api(Jackson.ModuleKotlin)

    compileOnly(Slf4jApi)

    implementation(Ktor2.ServerCore)
    implementation(Ktor2.Auth)
    implementation(NavFelles.TokenValidationKtor2)

    testImplementation(Jupiter.Api)
    testImplementation(Jupiter.Params)
    testRuntimeOnly(Jupiter.Engine)
    testImplementation(Kotest.AssertionsCore)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}