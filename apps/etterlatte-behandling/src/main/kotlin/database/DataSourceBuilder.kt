package no.nav.etterlatte.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class DataSourceBuilder(private val env: Map<String, String>) {
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = env["DB_JDBC_URL"] ?: String.format(
            "jdbc:postgresql://%s:%s/%s%s",
            requireNotNull(env["DB_HOST"]) { "database host must be set if jdbc url is not provided" },
            requireNotNull(env["DB_PORT"]) { "database port must be set if jdbc url is not provided" },
            requireNotNull(env["DB_DATABASE"]) { "database name must be set if jdbc url is not provided" },
            env["DB_USERNAME"]?.let { "?user=$it" } ?: ""
        )

        env["DB_USERNAME"]?.let { this.username = it }
        env["DB_PASSWORD"]?.let { this.password = it }
        setTransactionIsolation("TRANSACTION_SERIALIZABLE")
        initializationFailTimeout = 6000
    }

    val dataSource: DataSource
    init {
        if (!env.containsKey("DB_JDBC_URL")) {
            checkNotNull(env["DB_USERNAME"]) { "username must be set when vault is disabled" }
            checkNotNull(env["DB_PASSWORD"]) { "password must be set when vault is disabled" }
        }
        dataSource = HikariDataSource(hikariConfig)
    }

    fun migrate() = runMigration(dataSource)

    private fun runMigration(dataSource: DataSource) =
        Flyway.configure()
            .dataSource(dataSource)
            .apply {
                if (env.containsKey("NAIS_CLUSTER_NAME")) locations("db/migration", "db/gcp")
            }
            .load()
            .migrate()
}