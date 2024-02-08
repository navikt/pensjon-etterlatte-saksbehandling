package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.AbstractDatabaseExtension

object DatabaseExtension : AbstractDatabaseExtension() {
    override fun resetDb() {
        logger.info("Resetting database...")
        dataSource.connection.use {
            it.prepareStatement(
                """
                TRUNCATE hendelse CASCADE;
                TRUNCATE jobb CASCADE;
                ALTER SEQUENCE jobb_id_seq RESTART WITH 1;
                """.trimIndent(),
            ).execute()
        }
    }
}
