import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource



class DataSourceBuilder(private val env: Map<String, String>) {
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = jdbcUrl(
            host = env.required("DB_HOST"),
            port = env.required("DB_PORT"),
            databaseName = env.required("DB_DATABASE")
        )

        username = env.required("DB_USERNAME")
        password = env.required("DB_PASSWORD")
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

    fun migrate() =  runMigration(dataSource)


    private fun runMigration(dataSource: DataSource) =
        Flyway.configure()
            .dataSource(dataSource)
            .apply {
                if (env.containsKey("NAIS_CLUSTER_NAME")) locations("db/migration", "db/gcp")
            }
            .load()
            .migrate()
}

fun jdbcUrl(host: String, port: String, databaseName: String) =
    "jdbc:postgresql://${host}:$port/$databaseName"

fun Map<String, String>.required(property: String): String =
    requireNotNull(this[property]) { "Property $property was null" }
