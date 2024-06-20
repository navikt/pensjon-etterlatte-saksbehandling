package no.nav.etterlatte.tilbakekreving

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE tilbakekreving_hendelse;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
