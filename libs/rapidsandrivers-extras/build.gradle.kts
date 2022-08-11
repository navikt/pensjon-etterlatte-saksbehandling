import Logging.Slf4jApi

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")

}
dependencies {
    api(kotlin("stdlib"))
    api(kotlin("reflect"))

    api(Jackson.DatatypeJsr310)
    api(Jackson.DatatypeJdk8)
    api(Jackson.ModuleKotlin)
    api(NavFelles.RapidAndRiversKtor2)

    compileOnly(Slf4jApi)

    testImplementation(MockK.MockK)
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
