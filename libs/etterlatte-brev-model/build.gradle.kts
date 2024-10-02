plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation("no.nav.pensjon.brevbaker:brevbaker-api-model-common:1.4.0")
}
