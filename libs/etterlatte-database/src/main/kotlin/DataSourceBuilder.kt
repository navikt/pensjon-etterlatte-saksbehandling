package no.nav.etterlatte.libs.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import javax.sql.DataSource

object DataSourceBuilder {

    private const val MAX_POOL_SIZE = 10

    fun createDataSource(env: Map<String, String>, maxPoolSize: Int = MAX_POOL_SIZE): DataSource {
        val jdbcUrl = jdbcUrl(
            host = requireNotNull(env["DB_HOST"]),
            port = requireNotNull(env["DB_PORT"]).toInt(),
            databaseName = requireNotNull(env["DB_DATABASE"])
        )
        val username = requireNotNull(env["DB_USERNAME"])
        val password = requireNotNull(env["DB_PASSWORD"])
        return createDataSource(jdbcUrl, username, password, maxPoolSize)
    }

    fun createDataSource(
        jdbcUrl: String,
        username: String,
        password: String,
        maxPoolSize: Int = MAX_POOL_SIZE
    ): DataSource {
        val hikariConfig = HikariConfig().also {
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

fun jdbcUrl(host: String, port: Int, databaseName: String): String =
    "jdbc:postgresql://$host:$port/$databaseName"