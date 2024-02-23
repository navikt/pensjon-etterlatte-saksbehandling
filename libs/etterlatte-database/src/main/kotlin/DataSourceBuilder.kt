package no.nav.etterlatte.libs.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.isProd
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
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

fun DataSource.migrate(): MigrateResult =
    try {
        validateUniqueMigrationVersions()
        Flyway.configure()
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
        LoggerFactory.getLogger(this::class.java).error("Fikk feil under Flyway-migrering", e)
        throw e
    }

fun validateUniqueMigrationVersions() {
    val resourceFolder = readResources()

    val files = resourceFolder.listFiles()
    if (files == null) {
        throw RuntimeException("Failed to list files in the resources folder")
    } else {
        val filerMedPath =
            files.map { dir ->
                dir.listFiles()?.toList()?.map { it.path } ?: emptyList()
            }
        validateMigrationScriptVersions(filerMedPath)
    }
}

private fun readResources(): File {
    val systemClassLoader = ClassLoader.getSystemClassLoader()
    val resourceFolderURL: URL? = systemClassLoader.getResource("db")

    // Convert URL to file path
    val resourceFolderPath = resourceFolderURL?.file
    val resourceFolder = File(resourceFolderPath)

    // Check if it's a directory
    if (!resourceFolder.isDirectory) {
        throw RuntimeException("Fant ikke migreringsscript i resourceFolder for /db")
    }
    return resourceFolder
}

fun validateMigrationScriptVersions(files: List<List<String>>) {
    val allMigrationVersions =
        files.flatten()
            .filter { item -> item.endsWith(".sql") }
            .map { it.substring(it.lastIndexOf("/") + 1) }
            .map { it.substring(0, it.indexOf("__")) }
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
