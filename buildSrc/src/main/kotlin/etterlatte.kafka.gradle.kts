plugins {
    id("etterlatte.common")
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(Kafka.Clients)
    testImplementation(Kafka.EmbeddedEnv)
}
