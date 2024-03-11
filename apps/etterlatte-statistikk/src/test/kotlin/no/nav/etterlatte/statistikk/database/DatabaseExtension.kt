package no.nav.etterlatte.statistikk.database

import no.nav.etterlatte.GenerellDatabaseExtension
import no.nav.etterlatte.ResetDatabaseStatement

@ResetDatabaseStatement(
    """
    TRUNCATE TABLE sak;
    TRUNCATE TABLE soeknad_statistikk;
    TRUNCATE TABLE stoenad;
    TRUNCATE TABLE maanedsstatistikk_job;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
