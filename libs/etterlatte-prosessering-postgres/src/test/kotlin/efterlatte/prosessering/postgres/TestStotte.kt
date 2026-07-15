package efterlatte.prosessering.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

object TestStotte {
    fun startPostgres(): PostgreSQLContainer<*> =
        PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("prosessering")
            .withUsername("poc")
            .withPassword("poc")
            .also { it.start() }

    fun datasource(
        container: PostgreSQLContainer<*>,
        poolStorrelse: Int,
    ): DataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = container.jdbcUrl
                username = container.username
                password = container.password
                maximumPoolSize = poolStorrelse
            },
        )

    fun anvendSkjema(dataSource: DataSource) {
        val sql = TestStotte::class.java.getResource("/schema.sql")!!.readText()
        dataSource.connection.use { connection ->
            connection.createStatement().use { it.execute(sql) }
        }
    }

    /** Test-only: logger en eksekvering. UNIQUE-constraint på task_id fanger dobbel-kjøring. */
    fun loggEksekvering(
        dataSource: DataSource,
        taskId: Long,
        node: String,
    ) = dataSource.connection.use { connection ->
        connection.prepareStatement("INSERT INTO prosessering.execution_log (task_id, node) VALUES (?, ?)").use { statement ->
            statement.setLong(1, taskId)
            statement.setString(2, node)
            statement.executeUpdate()
        }
    }

    fun antallEksekveringer(dataSource: DataSource): Int =
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT count(*) FROM prosessering.execution_log").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }
        }
}
