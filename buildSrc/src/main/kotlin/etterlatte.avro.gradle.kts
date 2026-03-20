// Idl er deprecated i avro-compiler 1.12.1, men erstatningen (IdlReader) finnes ikke ennå.
// Fjern suppress når vi oppgraderer til en versjon med IdlReader.
@file:Suppress("DEPRECATION")

import org.apache.avro.compiler.idl.Idl
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData.StringType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

val avroSourceDir = file("src/main/avro")
val avroOutputDir = layout.buildDirectory.dir("generated-main-avro-java")

val generateAvroJava by tasks.registering {
    description = "Generates Java classes from Avro IDL (.avdl) files"
    inputs.dir(avroSourceDir)
    outputs.dir(avroOutputDir)

    doLast {
        val outDir = avroOutputDir.get().asFile
        outDir.mkdirs()

        fileTree(avroSourceDir).matching { include("**/*.avdl") }.files.forEach { avdlFile ->
            Idl(avdlFile).use { idl ->
                val protocol = idl.CompilationUnit()
                val compiler = SpecificCompiler(protocol)
                compiler.setStringType(StringType.String)
                compiler.compileToDestination(avdlFile, outDir)
            }
        }
    }
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
