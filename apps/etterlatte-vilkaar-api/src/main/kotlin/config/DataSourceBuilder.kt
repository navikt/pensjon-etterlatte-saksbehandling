package config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

class DataSourceBuilder(
    private val gcpProjectId: String,
    private val databaseRegion: String,
    private val databaseInstance: String,
    private val databaseUsername: String,
    private val databasePassword: String,
    private val databaseName: String
) {

    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = run {
            String.format(
                "jdbc:postgresql:///%s?%s&%s",
                databaseName,
                "cloudSqlInstance=$gcpProjectId:$databaseRegion:$databaseInstance",
                "socketFactory=com.google.cloud.sql.postgres.SocketFactory"
            )
        }
        initializationFailTimeout = 6000
        maximumPoolSize = 3
        maxLifetime = 30001
        username = databaseUsername
        password = databasePassword

        validate()
    }

    private val dataSource = HikariDataSource(hikariConfig)

    fun dataSource() = dataSource


}