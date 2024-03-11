package no.nav.etterlatte.brev

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE brev RESTART IDENTITY CASCADE;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
