package no.nav.etterlatte.libs.database

import no.nav.etterlatte.libs.common.appIsInGCP
import org.slf4j.Logger
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class SqlMaaHaaStorforbokstav(
    msg: String,
) : RuntimeException(msg)

class ManglerDobbelUnderscore(
    msg: String,
) : RuntimeException(msg)

class InvalidMigrationScriptVersion(
    versjon: String,
    antall: Int,
) : RuntimeException(
        "Kan ikke ha flere migreringer med samme versjon! Sjekk alle mapper under /resources/db. Versjon: $versjon, Antall: $antall",
    )

fun validateUniqueMigrationVersions(logger: Logger) {
    val filer = readResources(logger)

    validateMigrationScriptVersions(filer)
}

fun validateMigrationScriptVersions(files: List<String>) {
    val migreringsVersjonstallPaaSqlFil =
        files
            .filter { item -> item.endsWith(".sql") }
            .map { it.substring(it.lastIndexOf("/") + 1) }
            .map {
                val posisjonEtterDobbelUnderscore = it.indexOf("__")
                if (posisjonEtterDobbelUnderscore < 0) {
                    throw ManglerDobbelUnderscore("Sql fil mangler underscore, fil: $it")
                }
                val migreringsVersjonstallTmp = it.substring(0, posisjonEtterDobbelUnderscore)
                if (!migreringsVersjonstallTmp.first().isUpperCase()) {
                    throw SqlMaaHaaStorforbokstav("Sql fil mangler underscore, fil: $it")
                } else {
                    migreringsVersjonstallTmp
                }
            }

    val grupperte = migreringsVersjonstallPaaSqlFil.groupingBy { it }.eachCount()
    grupperte.forEach { (versjonOgType, antallDistinkteMigreringerAvVersjonOgType) ->
        if (antallDistinkteMigreringerAvVersjonOgType > 1) {
            throw InvalidMigrationScriptVersion(versjonOgType, antallDistinkteMigreringerAvVersjonOgType)
        }
    }
}

private fun readResources(logger: Logger): List<String> {
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    val resourceFolderURL: URL =
        systemClassLoader.getResource(
            "db",
        ) ?: throw RuntimeException("Fant ikke migreringsscript i resourceFolder for /db")

    logger.info("resourceFolderURL.path" + resourceFolderURL.path)
    return if (appIsInGCP()) {
        getPathsFromResourceJAR(
            logger,
        )
    } else {
        val files =
            File(resourceFolderURL.file).listFiles()
                ?: throw RuntimeException("Fant ingen filer i $resourceFolderURL listfiles er null")
        files
            .map { dir ->
                dir.listFiles()?.toList()?.map { it.path } ?: emptyList()
            }.flatten()
    }
}

private fun getPathsFromResourceJAR(logger: Logger): List<String> {
    val runtimeDirectory = System.getProperty("user.dir")
    val fileSet: MutableSet<String> = HashSet()
    Files.newDirectoryStream(Paths.get(runtimeDirectory)).use { stream ->
        for (path in stream) {
            if (!Files.isDirectory(path)) {
                fileSet.add(
                    path.fileName
                        .toString(),
                )
            }
        }
    }
    val manglerAppJar = fileSet.none { it == "app.jar" }
    if (manglerAppJar) {
        throw RuntimeException("app.jar ikke funnet i $runtimeDirectory")
    }
    val jarpath = "$runtimeDirectory/app.jar"
    logger.info("path to main jar resource $jarpath")
    val sqlFiler = mutableListOf<String>()
    val zip = ZipInputStream(FileInputStream(jarpath))
    zip.use {
        var entry: ZipEntry? = zip.getNextEntry()
        while (entry != null) {
            val filnavnMedPath = entry.name
            if (filnavnMedPath.startsWith("db/") && filnavnMedPath.endsWith(".sql")) {
                sqlFiler.addFirst(filnavnMedPath)
            }
            entry = zip.getNextEntry()
        }
    }

    return sqlFiler
}
