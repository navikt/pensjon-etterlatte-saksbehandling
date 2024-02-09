package no.nav.etterlatte.tidshendelser

@ResetDatabaseStatement(
    """
    TRUNCATE hendelse CASCADE;
    TRUNCATE jobb CASCADE;
    ALTER SEQUENCE jobb_id_seq RESTART WITH 1;
""",
)
class TidshendelserDatabaseExtension : DatabaseExtension()
