package no.nav.etterlatte.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

sealed class JdbcUrlBuilder {
    abstract fun build(): String
}

class StandardJdbcUrlBuilder(
    private val jdbcUrl: String,
) : JdbcUrlBuilder() {
    override fun build() = jdbcUrl
}

class GcpJdbcUrlBuilder(
    private val gcpProjectId: String,
    private val databaseRegion: String,
    private val databaseInstance: String,
    private val databaseName: String,
) : JdbcUrlBuilder() {
    override fun build(): String =
        run {
            String.format(
                "jdbc:postgresql:///%s?%s&%s",
                databaseName,
                "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
                "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
            )
        }
}


class DataSourceBuilder(
    private val jdbcUrlBuilder: JdbcUrlBuilder,
    private val databaseUsername: String,
    private val databasePassword: String,
) {

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = jdbcUrlBuilder.build()
        username = databaseUsername
        password = databasePassword

        initializationFailTimeout = 6000
        maximumPoolSize = 3
        maxLifetime = 30001

        validate()
    }

    private val dataSource = HikariDataSource(hikariConfig)

    fun dataSource() = dataSource

}