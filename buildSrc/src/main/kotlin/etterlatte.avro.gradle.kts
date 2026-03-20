import no.nav.etterlatte.GenerateAvroJava
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

val avroOutputDir = layout.buildDirectory.dir("generated-main-avro-java")

val generateAvroJava by tasks.registering(GenerateAvroJava::class) {
    description = "Generates Java classes from Avro IDL (.avdl) files"
    sourceDir.set(file("src/main/avro"))
    outputDir.set(avroOutputDir)
}

pluginManager.withPlugin("java") {
    the<SourceSetContainer>().named("main") {
        java.srcDir(avroOutputDir)
    }
    tasks.named("compileJava").configure { dependsOn(generateAvroJava) }
}

plugins.withType<KotlinPluginWrapper> {
    tasks.named("compileKotlin").configure { dependsOn(generateAvroJava) }
    tasks.named("compileTestKotlin").configure { dependsOn(generateAvroJava) }
}
