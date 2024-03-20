package no.nav.etterlatte.migrering

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE TABLE saker_til_migrering;
    TRUNCATE TABLE feilkjoering;
    TRUNCATE TABLE dryrun;
    TRUNCATE TABLE pesyssak CASCADE;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
