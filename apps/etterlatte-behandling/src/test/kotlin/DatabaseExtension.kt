package no.nav.etterlatte

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Det tar veldig mye tid å kjøre opp stadig nye Postgres-containere og kjøre Flyway migreringer.
 * Denne extensionen kjører opp èn instans, som så gjenbrukes av de som måtte ønske det.
 */
object DatabaseExtension : BeforeAllCallback, AfterAllCallback, ExtensionContext.Store.CloseableResource {
    override fun beforeAll(context: ExtensionContext) {
        GenerellDatabaseExtension.beforeAll(context)
    }

    override fun afterAll(context: ExtensionContext) {
        GenerellDatabaseExtension.afterAll(RESET_DATABASE)
    }

    override fun close() {
        GenerellDatabaseExtension.close()
    }

    val dataSource = GenerellDatabaseExtension.dataSource

    val postgreSQLContainer = GenerellDatabaseExtension.postgreSQLContainer

    private const val RESET_DATABASE = """
                        TRUNCATE behandling CASCADE;
                        TRUNCATE behandlinghendelse CASCADE;
                        TRUNCATE grunnlagsendringshendelse CASCADE;
                        TRUNCATE sak CASCADE;
                        TRUNCATE oppgave CASCADE;
                        
                        ALTER SEQUENCE behandlinghendelse_id_seq RESTART WITH 1;
                        ALTER SEQUENCE sak_id_seq RESTART WITH 1;
                        """

    fun resetDb() = GenerellDatabaseExtension.resetDb(RESET_DATABASE)
}
