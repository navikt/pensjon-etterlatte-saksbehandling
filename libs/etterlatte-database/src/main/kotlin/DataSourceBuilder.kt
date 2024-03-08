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
import java.io.FileInputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.sql.DataSource

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
    val filer = readResources(logger)

    validateMigrationScriptVersions(filer)
}

private fun getPathsFromResourceJAR(
    jarpath: String,
    logger: Logger,
): List<String> {
    logger.info(jarpath)
    val sqlFiler: List<String> = emptyList()
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

private fun readResources(logger: Logger): List<String> {
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    val resourceFolderURL: URL =
        systemClassLoader.getResource(
            "db",
        ) ?: throw RuntimeException("Fant ikke migreringsscript i resourceFolder for /db")

    logger.info("resourceFolderURL.path" + resourceFolderURL.path)
    return if (appIsInGCP()) {
        getPathsFromResourceJAR(
            DataSource::class.java.getProtectionDomain().codeSource.location
                .toURI().path,
            logger,
        )
    } else {
        val files =
            File(resourceFolderURL.file).listFiles()
                ?: throw RuntimeException("Fant ingen filer i $resourceFolderURL listfiles er null")
        files.map { dir ->
            dir.listFiles()?.toList()?.map { it.path } ?: emptyList()
        }.flatten()
    }
}

fun validateMigrationScriptVersions(files: List<String>) {
    val allMigrationVersions =
        files
            .filter { item -> item.endsWith(".sql") }
            .map { it.substring(it.lastIndexOf("/") + 1) }
            .map {
                val pos = it.indexOf("__")
                if (pos < 0) {
                    throw RuntimeException("Sql script mangler underscore, fil: $it")
                }
                it.substring(0, pos)
            }

    val grupperte = allMigrationVersions.groupingBy { it }.eachCount()
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
