package no.nav.etterlatte

import com.zaxxer.hikari.HikariDataSource
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

object GenerellDatabaseExtension {
    private val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)

    val postgreSQLContainer =
        PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
            .also { logger.info("Starting shared Postgres testcontainer") }
            .also { it.start() }

    private val ds: DataSource =
        DataSourceBuilder.createDataSource(
            jdbcUrl = postgreSQLContainer.jdbcUrl,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password,
        ).apply { migrate() }

    private val connections = mutableListOf<Connection>()

    fun beforeAll(context: ExtensionContext) {
        context.root.getStore(GLOBAL).put("postgres-testdb", this)
    }

    /**
     * Ikke gå tom for tilkoblinger, så kast ut alle som er ferdige med jobben sin
     */
    fun afterAll(resetDatabase: String) {
        resetDb(resetDatabase)

        connections.forEach {
            (ds as HikariDataSource).evictConnection(it)
        }
        connections.clear()
    }

    val dataSource: DataSource
        get() = DataSourceWrapper(ds, connections::add)

    /**
     * Trigges av rammeverket når siste testinstans er kjørt.
     */
    fun close() {
        logger.info("Stopping shared Postgres testcontainer")
        postgreSQLContainer.stop()
    }

    /**
     * Sikre at hver testklasse starter med en fresh database, i det minste for sentrale tabeller
     */
    fun resetDb(sql: String) {
        logger.info("Resetting database...")
        dataSource.connection.use {
            it.prepareStatement(sql.trimIndent()).execute()
        }
    }

    /**
     * Wrappe slik at når konsument ber om ny connection så kan den tas vare på mtp eviction
     */
    private class DataSourceWrapper(val datasource: DataSource, val collector: (Connection) -> Unit) : DataSource {
        override fun getLogWriter(): PrintWriter = datasource.logWriter

        override fun setLogWriter(out: PrintWriter?) {
            datasource.logWriter = out
        }

        override fun setLoginTimeout(seconds: Int) {
            datasource.loginTimeout = seconds
        }

        override fun getLoginTimeout(): Int = datasource.loginTimeout

        override fun getParentLogger(): Logger = datasource.parentLogger

        override fun <T : Any?> unwrap(iface: Class<T>?): T = datasource.unwrap(iface)

        override fun isWrapperFor(iface: Class<*>?): Boolean = datasource.isWrapperFor(iface)

        override fun getConnection(): Connection = datasource.connection.also { collector.invoke(it) }

        override fun getConnection(
            username: String?,
            password: String?,
        ): Connection = datasource.getConnection(username, password).also { collector.invoke(it) }
    }
}
