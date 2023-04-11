

plugins {
    id("etterlatte.common")
    id("etterlatte.postgres")
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

    testImplementation(libs.navfelles.tokenvalidationktor2)
    testImplementation(libs.test.mockk)
    testImplementation(libs.ktor2.servertests)
    testImplementation(libs.navfelles.mockoauth2server)
    testImplementation(libs.test.kotest.assertionscore)

    testImplementation(project(":libs:testdata"))
}