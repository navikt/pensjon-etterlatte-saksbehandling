package no.nav.etterlatte

/**
 * Det tar veldig mye tid å kjøre opp stadig nye Postgres-containere og kjøre Flyway migreringer.
 * Denne extensionen kjører opp èn instans, som så gjenbrukes av de som måtte ønske det.
 */
object DatabaseExtension : AbstractDatabaseExtension() {
    override fun resetDb() {
        logger.info("Resetting database...")
        dataSource.connection.use {
            it.prepareStatement(
                """
                TRUNCATE behandling CASCADE;
                TRUNCATE behandlinghendelse CASCADE;
                TRUNCATE grunnlagsendringshendelse CASCADE;
                TRUNCATE sak CASCADE;
                TRUNCATE oppgave CASCADE;
                
                ALTER SEQUENCE behandlinghendelse_id_seq RESTART WITH 1;
                ALTER SEQUENCE sak_id_seq RESTART WITH 1;
                """.trimIndent(),
            ).execute()
        }
    }
}
