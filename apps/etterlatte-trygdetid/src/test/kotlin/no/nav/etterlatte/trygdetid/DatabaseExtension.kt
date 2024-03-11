package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE trygdetid CASCADE;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
