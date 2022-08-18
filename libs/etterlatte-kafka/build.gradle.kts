import Logging.Slf4jApi

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(Jackson.DatatypeJsr310)
    api(Jackson.DatatypeJdk8)
    api(Jackson.ModuleKotlin)

    compileOnly(Slf4jApi)

    testImplementation(Jupiter.Api)
    testImplementation(Jupiter.Params)
    testRuntimeOnly(Jupiter.Engine)
    testImplementation(Kotest.AssertionsCore)
    implementation(Kafka.Clients)
    testImplementation(Kafka.EmbeddedEnv)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }
}