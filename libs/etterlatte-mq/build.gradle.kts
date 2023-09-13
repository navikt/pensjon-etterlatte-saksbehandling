plugins {
    kotlin("jvm")
    id("etterlatte.libs")
    id("java-library")
    id("java-test-fixtures")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(libs.mq.jakarta.client)
    implementation(libs.messaginghub.pooled.jms)

    testFixturesImplementation(project(":libs:saksbehandling-common"))
    testFixturesImplementation(libs.mq.jakarta.client)
    testFixturesImplementation(libs.messaginghub.pooled.jms)
    testFixturesImplementation(libs.test.mockk)
    testFixturesImplementation(libs.kotlinx.coroutinescore)
}