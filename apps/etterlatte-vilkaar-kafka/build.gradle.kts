
plugins {
    id("etterlatte.rapids-and-rivers")
}

dependencies {
    implementation(project(":libs:common"))
    implementation(Jackson.DatatypeJsr310)
    implementation(Jackson.DatatypeJdk8)
    implementation(Jackson.ModuleKotlin)
    testImplementation(MockK.MockK)
}
