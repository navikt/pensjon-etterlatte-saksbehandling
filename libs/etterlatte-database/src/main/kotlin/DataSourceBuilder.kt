package no.nav.etterlatte.libs.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.etterlatte.libs.common.Miljoevariabler
import no.nav.etterlatte.libs.common.appIsInGCP
import no.nav.etterlatte.libs.common.isDev
import no.nav.etterlatte.libs.common.isProd
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory
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

fun jdbcUrl(
    host: String,
    port: Int,
    databaseName: String,
): String = "jdbc:postgresql://$host:$port/$databaseName"
