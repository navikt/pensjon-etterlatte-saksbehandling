package no.nav.etterlatte

import com.zaxxer.hikari.HikariDataSource
import no.nav.etterlatte.libs.database.DataSourceBuilder
import no.nav.etterlatte.libs.database.POSTGRES_VERSION
import no.nav.etterlatte.libs.database.migrate
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Det tar veldig mye tid å kjøre opp stadig nye Postgres-containere og kjøre Flyway migreringer.
 * Denne extensionen kjører opp èn instans, som så gjenbrukes av de som måtte ønske det.
 * <p>
 * Benytt @ResetDatabaseStatement i en subklasse for å angi SQL for å tømme databasen.
 */
open class GenerellDatabaseExtension :
    AfterAllCallback,
    ExtensionContext.Store.CloseableResource,
    ParameterResolver {
    companion object {
        val logger: org.slf4j.Logger = LoggerFactory.getLogger(this::class.java)
        private val postgreSQLContainer =
            PostgreSQLContainer<Nothing>("postgres:$POSTGRES_VERSION")
                .also { logger.info("Starting shared Postgres testcontainer") }
                .also { it.start() }

        private val ds: DataSource =
            DataSourceBuilder
                .createDataSource(
                    jdbcUrl = postgreSQLContainer.jdbcUrl,
                    username = postgreSQLContainer.username,
                    password = postgreSQLContainer.password,
                ).apply { migrate(processExiter = { }) }
    }

    private val connections = mutableListOf<Connection>()

    /**
     * Ikke gå tom for tilkoblinger, så kast ut alle som er ferdige med jobben sin
     */
    override fun afterAll(context: ExtensionContext) {
        resetDb()

        connections.forEach {
            (ds as HikariDataSource).evictConnection(it)
        }
        connections.clear()
    }

    private val dataSource: DataSource
        get() = DataSourceWrapper(ds, connections::add)

    /**
     * Trigges av rammeverket når siste testinstans er kjørt.
     */
    override fun close() {
        logger.info("Stopping shared Postgres testcontainer")
        postgreSQLContainer.stop()
    }

    /**
     * Sikre at hver testklasse starter med en fresh database
     */
    fun resetDb() {
        (
            this::class.java.annotations
                .find { it.annotationClass == ResetDatabaseStatement::class } as? ResetDatabaseStatement
        )?.let { annotation ->
            ds.connection.use {
                logger.info("Resetting database...")
                it.createStatement().execute(annotation.statement)
            }
        }
            ?: {
                logger.info("Skipper reset av database, @ResetDatabaseStatement ikke funnet.")
            }
    }

    fun properties() =
        PostgresProperties(
            databaseName = postgreSQLContainer.databaseName,
            host = postgreSQLContainer.host,
            firstMappedPort = postgreSQLContainer.firstMappedPort,
            username = postgreSQLContainer.username,
            password = postgreSQLContainer.password,
        )

    /**
     * Wrappe slik at når konsument ber om ny connection så kan den tas vare på mtp eviction
     */
    private class DataSourceWrapper(
        val datasource: DataSource,
        val collector: (Connection) -> Unit,
    ) : DataSource {
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

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean = parameterContext.parameter?.type == DataSource::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any =
        if (parameterContext.parameter?.type == DataSource::class.java) {
            dataSource
        } else {
            throw IllegalArgumentException("Kan ikke resolve parameter av type ${parameterContext.parameter?.type}")
        }
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ResetDatabaseStatement(
    val statement: String,
)
