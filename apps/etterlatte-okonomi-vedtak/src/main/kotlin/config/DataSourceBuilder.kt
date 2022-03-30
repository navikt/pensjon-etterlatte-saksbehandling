package no.nav.etterlatte.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

class DataSourceBuilder(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String,
) {
    private val hikariConfig  = HikariConfig().also {
        it.jdbcUrl = jdbcUrl
        it.username = username
        it.password = password
        it.transactionIsolation = "TRANSACTION_SERIALIZABLE"
        it.initializationFailTimeout = -1

        it.maximumPoolSize = 3
        it.minimumIdle = 1
        it.idleTimeout = 10001
        it.maxLifetime = 30001
    }

    fun dataSource() = HikariDataSource(hikariConfig)

    fun migrate() =
        Flyway.configure()
            .dataSource(dataSource())
            .load()
            .migrate()

}