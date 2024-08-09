plugins {
    id("etterlatte.common")
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-behandling-model"))
    implementation(project(":libs:etterlatte-beregning-model"))
    implementation(project(":libs:etterlatte-brev-model"))
    implementation(project(":libs:etterlatte-migrering-model"))
    implementation(project(":libs:etterlatte-oppgave-model"))
    implementation(project(":libs:etterlatte-vedtaksvurdering-model"))
    implementation(project(":libs:etterlatte-vilkaarsvurdering-model"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:rapidsandrivers-extras"))
    implementation(libs.cache.caffeine)
}
