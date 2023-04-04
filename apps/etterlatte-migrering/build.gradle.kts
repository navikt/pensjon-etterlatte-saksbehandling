plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
    id("etterlatte.rapids-and-rivers-ktor2")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:saksbehandling-common"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))
    implementation(project(":libs:ktor2client-onbehalfof"))

    implementation(libs.ktor2.servercio)
    implementation(libs.database.kotliquery)
    testImplementation(libs.test.mockk)
}