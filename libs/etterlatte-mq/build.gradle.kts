plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(libs.mq.jakarta.client)
    implementation(libs.messaginghub.pooled.jms)
}