class DataSourceBuilder(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String
) {
    private val hikariConfig = HikariConfig().also {
        it.jdbcUrl = jdbcUrl
        it.username = username
        it.password = password
        it.transactionIsolation = "TRANSACTION_SERIALIZABLE"
        it.initializationFailTimeout = 6000
        it.maximumPoolSize = 3

        it.validate()
    }

    private val dataSource = HikariDataSource(hikariConfig)

    fun dataSource() = dataSource

    fun migrate() =
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
}