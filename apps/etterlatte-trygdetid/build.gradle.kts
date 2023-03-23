plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":libs:common"))
    implementation(project(":libs:etterlatte-helsesjekk"))
    implementation(project(":libs:etterlatte-ktor"))
    implementation(project(":libs:etterlatte-database"))

    implementation(libs.ktor2.servercio)
    implementation(libs.database.kotliquery)

    testImplementation(libs.test.kotest.assertionscore)
}