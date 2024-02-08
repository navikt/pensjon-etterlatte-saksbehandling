package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.AbstractDatabaseExtension

object DatabaseExtension : AbstractDatabaseExtension(
    """
                TRUNCATE hendelse CASCADE;
                TRUNCATE jobb CASCADE;
                ALTER SEQUENCE jobb_id_seq RESTART WITH 1;
                """,
)
