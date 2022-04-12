import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource



class DataSourceBuilder(config: HikariConfig) {
    constructor(env: Map<String, String>) : this( HikariConfig().apply {
        jdbcUrl = jdbcUrl(
            host = env.required("DB_HOST"),
            port = env.required("DB_PORT"),
            databaseName = env.required("DB_DATABASE")
        )

        username = env.required("DB_USERNAME")
        password = env.required("DB_PASSWORD")

    })

    val dataSource: DataSource
    init {

        dataSource = HikariDataSource(config.apply {
            setTransactionIsolation("TRANSACTION_SERIALIZABLE")
            initializationFailTimeout = 6000

        })
    }

    fun migrate() =  runMigration(dataSource)

    private fun runMigration(dataSource: DataSource) =
        Flyway.configure()
            .dataSource(dataSource)
            .load()
            .migrate()
}

fun jdbcUrl(host: String, port: String, databaseName: String) =
    "jdbc:postgresql://${host}:$port/$databaseName"

fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }
