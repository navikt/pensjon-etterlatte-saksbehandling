plugins {
    id("etterlatte.common")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(NavFelles.RapidAndRiversKtor2)
    implementation(project(":libs:rapidsandrivers-extras"))
}