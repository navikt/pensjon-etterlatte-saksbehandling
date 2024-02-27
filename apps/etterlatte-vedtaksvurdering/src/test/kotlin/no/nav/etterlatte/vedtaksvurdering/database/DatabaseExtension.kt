package no.nav.etterlatte.vedtaksvurdering.database

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE vedtak CASCADE;
    TRUNCATE utbetalingsperiode CASCADE;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
