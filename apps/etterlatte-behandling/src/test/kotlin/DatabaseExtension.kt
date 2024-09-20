package no.nav.etterlatte

@ResetDatabaseStatement(
    """
    TRUNCATE behandling CASCADE;
    TRUNCATE behandlinghendelse CASCADE;
    TRUNCATE grunnlagsendringshendelse CASCADE;
    TRUNCATE sak CASCADE;
    TRUNCATE oppgave CASCADE;
    TRUNCATE tilbakekrevingsperiode CASCADE;
    TRUNCATE tilbakekreving CASCADE;
    TRUNCATE vilkaarsvurdering CASCADE;
    
    ALTER SEQUENCE behandlinghendelse_id_seq RESTART WITH 1;
    ALTER SEQUENCE sak_id_seq RESTART WITH 1;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
