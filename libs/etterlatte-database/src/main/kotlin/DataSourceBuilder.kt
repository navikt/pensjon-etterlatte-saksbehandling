package no.nav.etterlatte.libs.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.etterlatte.libs.common.requireEnvValue
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object DataSourceBuilder {
    private const val MAX_POOL_SIZE = 10

    fun createDataSource(
        env: Map<String, String>,
        maxPoolSize: Int = MAX_POOL_SIZE,
    ): DataSource {
        val jdbcUrl =
            jdbcUrl(
                host = env.requireEnvValue("DB_HOST"),
                port = env.requireEnvValue("DB_PORT").toInt(),
                databaseName = env.requireEnvValue("DB_DATABASE"),
            )
        val username = env.requireEnvValue("DB_USERNAME")
        val password = env.requireEnvValue("DB_PASSWORD")
        return createDataSource(jdbcUrl, username, password, maxPoolSize)
    }

    fun createDataSource2(env: Map<String, String>): DataSource = createDataSource(ApplicationProperties.fromEnv(env))

    fun createDataSource(properties: ApplicationProperties) =
        createDataSource(
            jdbcUrl = properties.jdbcUrl,
            username = properties.dbUsername,
            password = properties.dbUsername,
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

fun DataSource.migrate(gcp: Boolean = true): MigrateResult =
    try {
        Flyway.configure()
            .dataSource(this)
            .apply {
                // Kjør GCP-spesifikke migrasjoner kun hvis vi er i GCP
                if (gcp) {
                    locations("db/migration", "db/gcp")
                }
            }
            .load()
            .migrate()
    } catch (e: Exception) {
        LoggerFactory.getLogger(this::class.java).error("Fikk feil under Flyway-migrering", e)
        throw e
    }

fun jdbcUrl(
    host: String,
    port: Int,
    databaseName: String,
): String = "jdbc:postgresql://$host:$port/$databaseName"
