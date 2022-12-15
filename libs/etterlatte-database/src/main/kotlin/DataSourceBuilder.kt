package no.nav.etterlatte.libs.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import javax.sql.DataSource

object DataSourceBuilder {
    fun createDataSource(env: Map<String, String>): DataSource {
        val jdbcUrl = jdbcUrl(
            host = requireNotNull(env["DB_HOST"]),
            port = requireNotNull(env["DB_PORT"]).toInt(),
            databaseName = requireNotNull(env["DB_DATABASE"])
        )
        val username = requireNotNull(env["DB_USERNAME"])
        val password = requireNotNull(env["DB_PASSWORD"])
        return createDataSource(jdbcUrl, username, password)
    }

    fun createDataSource(jdbcUrl: String, username: String, password: String): DataSource {
        val hikariConfig = HikariConfig().also {
            it.jdbcUrl = jdbcUrl
            it.username = username
            it.password = password
            it.transactionIsolation = "TRANSACTION_SERIALIZABLE"
            it.initializationFailTimeout = 6000
            it.maximumPoolSize = 3
            it.validate()
        }
        return HikariDataSource(hikariConfig)
    }
}

fun DataSource.migrate(): MigrateResult =
    Flyway.configure()
        .dataSource(this)
        .load()
        .migrate()

fun jdbcUrl(host: String, port: Int, databaseName: String): String =
    "jdbc:postgresql://$host:$port/$databaseName"