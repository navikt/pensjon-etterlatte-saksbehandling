plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-beregning-model"))
    implementation(project(":libs:etterlatte-brev-model"))
    implementation(project(":libs:etterlatte-migrering-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(libs.ktor2.clientcore)
    implementation(libs.ktor2.clientauth)
    implementation(libs.ktor2.clientjackson)
    implementation(libs.ktor2.clientcontentnegotiation)
}
