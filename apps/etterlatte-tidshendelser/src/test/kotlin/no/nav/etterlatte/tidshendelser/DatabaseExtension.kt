package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE hendelse CASCADE;
    TRUNCATE jobb CASCADE;
    ALTER SEQUENCE jobb_id_seq RESTART WITH 1;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
