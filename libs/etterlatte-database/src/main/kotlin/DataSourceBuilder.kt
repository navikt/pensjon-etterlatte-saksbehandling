package no.nav.etterlatte.libs.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.isProd
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import javax.sql.DataSource

val logge2weer = LoggerFactory.getLogger(DataSourceBuilder::class.java)

object DataSourceBuilder {
    private const val MAX_POOL_SIZE = 10

    fun createDataSource(env: Map<String, String>): DataSource = createDataSource(ApplicationProperties.fromEnv(env))

    fun createDataSource(properties: ApplicationProperties) =
        createDataSource(
            jdbcUrl = properties.jdbcUrl,
            username = properties.dbUsername,
            password = properties.dbPassword,
        )

    fun createDataSource(
        jdbcUrl: String,
        username: String,
        password: String,
        maxPoolSize: Int = MAX_POOL_SIZE,
    ): DataSource {
        logge2weer.info("ds + $jdbcUrl + $username + $password")
        val hikariConfig =
            HikariConfig().also {
                it.jdbcUrl = jdbcUrl
                it.username = username
                it.password = password
                it.transactionIsolation = "TRANSACTION_SERIALIZABLE"
                it.initializationFailTimeout = 6000
                it.maximumPoolSize = maxPoolSize
                it.validate()
            }
        return HikariDataSource(hikariConfig)
    }
}

fun DataSource.migrate(): MigrateResult {
    val logger = LoggerFactory.getLogger(this::class.java)
    try {
        validateUniqueMigrationVersions(logger)
        return Flyway.configure()
            .dataSource(this)
            .apply {
                val dblocationsMiljoe = mutableListOf("db/migration")
                if (appIsInGCP()) {
                    dblocationsMiljoe.add("db/gcp")
                }
                if (isProd()) {
                    dblocationsMiljoe.add("db/prod")
                }
                locations(*dblocationsMiljoe.toTypedArray())
            }
            .load()
            .migrate()
    } catch (e: Exception) {
        logger.error("Fikk feil under Flyway-migrering", e)
        throw e
    }
}

fun validateUniqueMigrationVersions(logger: Logger) {
    val resourceFolder = readResources(logger)

    val files = resourceFolder.listFiles()
    println("******files " + files)
    if (files == null) {
        throw RuntimeException("Failed to list files in the resources folder")
    } else {
        val filerMedPath =
            files.map { dir ->
                println("listfilespath " + dir.path + " files" + dir.listFiles())
                dir.listFiles()?.toList()?.map { it.path } ?: emptyList()
            }
        validateMigrationScriptVersions(filerMedPath)
    }
}

private fun getPathsFromResourceJAR(
    folder: String,
    jarpath: String,
): List<List<String>>? {
    // file walks JAR
    println(jarpath)
    val uri: URI = URI.create(jarpath)
    println(uri)
    val attributes = hashMapOf("create" to "false")
    var filer: List<List<String>>? = null
    FileSystems.newFileSystem(uri, attributes).use { fs ->
        val dbFolder = File(fs.getPath(folder).toString())
        val files = dbFolder.listFiles()
        val filerMedPath =
            files?.map { dir ->
                println("jar listfilespath " + dir.path + " files" + dir.listFiles())
                dir.listFiles()?.toList()?.map { it.path } ?: emptyList()
            }
        filer = filerMedPath
        println("jar files $filerMedPath")
        Files.walk(fs.getPath(folder))
            .filter { it != null }
            .map { path: Path? ->
                val file = File(path.toString())
                if (file.isDirectory) {
                    println("is dir " + file.path)
                    println(file.listFiles())
                }
                Files.isRegularFile(
                    path,
                )
            }
            .collect(Collectors.toList())
    }
    return filer
}

private fun readResources(logger: Logger): File {
    logger.info("readResources")
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    val resourceFolderURL: URL =
        systemClassLoader.getResource(
            "db",
        ) ?: throw RuntimeException("Fant ikke migreringsscript i resourceFolder for /db")

    logger.info("resourceFolderURL.path" + resourceFolderURL.path)
    if (resourceFolderURL.path.toString().contains("jar:")) {
        val pathsFromResourceJAR = getPathsFromResourceJAR("db", resourceFolderURL.path)
        val filenames =
            pathsFromResourceJAR.map {
                var filePathInJAR = it.toString()
                // Windows will returns /json/file1.json, cut the first /
                // the correct path should be json/file1.json
                if (filePathInJAR.startsWith("/")) {
                    filePathInJAR = filePathInJAR.substring(1, filePathInJAR.length)
                }

                logger.info("filePathInJAR : $filePathInJAR")

                // read a file from resource folder
                filePathInJAR
            }
        filenames
    }

    val resourceFolder = File(resourceFolderURL.file)
    return resourceFolder
}

fun validateMigrationScriptVersions(files: List<List<String>>) {
    println("validateMigrationScriptVersions Files: " + files)
    val allMigrationVersions =
        files.flatten()
            .filter { item ->
                val harSqlSuffix = item.endsWith(".sql")
                // TODO: lag validator
                harSqlSuffix
            }
            .map { it.substring(it.lastIndexOf("/") + 1) }
            .map {
                val pos = it.indexOf("__")
                if (pos < 0) {
                    throw RuntimeException("Sql script mangler underscore, fil: $it")
                }
                it.substring(0, pos)
            }

    val migreringerSomListe = allMigrationVersions.toList()
    val grupperte = migreringerSomListe.groupingBy { it }.eachCount()
    grupperte.forEach {
        if (it.value > 1) {
            throw RuntimeException(
                "Kan ikke ha flere migreringer med samme versjon! Sjekk alle mapper under /resources/db. Versjon: ${it.key}",
            )
        }
    }
}

fun jdbcUrl(
    host: String,
    port: Int,
    databaseName: String,
): String = "jdbc:postgresql://$host:$port/$databaseName"
