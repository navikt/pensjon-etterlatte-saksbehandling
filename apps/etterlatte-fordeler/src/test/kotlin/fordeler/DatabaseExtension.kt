package no.nav.etterlatte.fordeler

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE kriterietreff, fordelinger;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
