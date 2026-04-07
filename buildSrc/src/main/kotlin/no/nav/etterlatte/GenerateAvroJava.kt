// Idl er deprecated i avro-compiler 1.12.1, men erstatningen (IdlReader) finnes ikke ennå.
// Fjern suppress når vi oppgraderer til en versjon med IdlReader.
@file:Suppress("DEPRECATION")

package no.nav.etterlatte

import org.apache.avro.compiler.idl.Idl
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData.StringType
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateAvroJava : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val outDir = outputDir.get().asFile
        if (outDir.exists()) {
            outDir.deleteRecursively()
        }
        outDir.mkdirs()

        sourceDir.asFileTree.matching { include("**/*.avdl") }.files.forEach { avdlFile ->
            Idl(avdlFile).use { idl ->
                val protocol = idl.CompilationUnit()
                val compiler = SpecificCompiler(protocol)
                compiler.setStringType(StringType.String)
                compiler.compileToDestination(avdlFile, outDir)
            }
        }
    }
}
