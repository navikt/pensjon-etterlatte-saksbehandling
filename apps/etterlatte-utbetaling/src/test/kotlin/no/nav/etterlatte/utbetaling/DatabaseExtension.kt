package no.nav.etterlatte.utbetaling

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE utbetaling CASCADE;
    TRUNCATE avstemming;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
