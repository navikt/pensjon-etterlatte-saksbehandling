package no.nav.etterlatte.libs.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.isDev
import no.nav.etterlatte.libs.common.isProd
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.sql.DataSource
import kotlin.system.exitProcess

object DataSourceBuilder {
    private const val MAX_POOL_SIZE = 10

    fun createDataSource(env: Miljoevariabler): DataSource = createDataSource(ApplicationProperties.fromEnv(env))

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

fun DataSource.migrate(processExiter: () -> Unit = { exitProcess(1) }): MigrateResult {
    val logger = LoggerFactory.getLogger(this::class.java)
    try {
        validateUniqueMigrationVersions(logger)
        return Flyway
            .configure()
            .dataSource(this)
            .apply {
                val dblocationsMiljoe = mutableListOf("db/migration")
                if (appIsInGCP()) {
                    dblocationsMiljoe.add("db/gcp")
                }
                if (isDev() || !appIsInGCP()) {
                    dblocationsMiljoe.add("db/dev")
                }
                if (isProd()) {
                    dblocationsMiljoe.add("db/prod")
                }
                locations(*dblocationsMiljoe.toTypedArray())
            }.load()
            .migrate()
    } catch (e: InvalidMigrationScriptVersion) {
        logger.error("Ugyldig versjon på migreringsscript", e)
        processExiter()
        throw e // Vil berre slå til under test, i prod-kode vil processExiter-kallet gjera exitProcess
    } catch (e: Exception) {
        logger.error("Fikk feil under Flyway-migrering", e)
        throw e
    }
}

fun validateUniqueMigrationVersions(logger: Logger) {
    val filer = readResources(logger)

    validateMigrationScriptVersions(filer)
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

fun validateMigrationScriptVersions(files: List<String>) {
    val migreringsVersjonstallPaaSqlFil =
        files
            .filter { item -> item.endsWith(".sql") }
            .map { it.substring(it.lastIndexOf("/") + 1) }
            .map {
                val posisjonEtterDobbelUnderscore = it.indexOf("__")
                if (posisjonEtterDobbelUnderscore < 0) {
                    throw MangerDobbelUnderscore("Sql fil mangler underscore, fil: $it")
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

fun jdbcUrl(
    host: String,
    port: Int,
    databaseName: String,
): String = "jdbc:postgresql://$host:$port/$databaseName"

class SqlMaaHaaStorforbokstav(
    msg: String,
) : RuntimeException(msg)

class MangerDobbelUnderscore(
    msg: String,
) : RuntimeException(msg)

class InvalidMigrationScriptVersion(
    versjon: String,
    antall: Int,
) : RuntimeException(
        "Kan ikke ha flere migreringer med samme versjon! Sjekk alle mapper under /resources/db. Versjon: $versjon, Antall: $antall",
    )
