plugins {
    id("etterlatte.common")
    id("etterlatte.rapids-and-rivers-ktor2")
    id("etterlatte.postgres")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-jobs"))
    implementation(project(":libs:ktor2client-auth-clientcredentials"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:etterlatte-ktor"))

    implementation(Ktor2.ClientCore)

    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)

    testImplementation(MockK.MockK)
    testImplementation(Kotlinx.CoroutinesCore)
}