package no.nav.etterlatte

@ResetDatabaseStatement(
    """
    TRUNCATE behandling CASCADE;
    TRUNCATE behandlinghendelse CASCADE;
    TRUNCATE grunnlagsendringshendelse CASCADE;
    TRUNCATE sak CASCADE;
    TRUNCATE saksendring CASCADE;
    TRUNCATE oppgave CASCADE;
    TRUNCATE tilbakekrevingsperiode CASCADE;
    TRUNCATE tilbakekreving CASCADE;
    TRUNCATE vilkaarsvurdering CASCADE;
    TRUNCATE vedtak CASCADE;
    TRUNCATE utbetalingsperiode CASCADE;
    TRUNCATE prosessering_task_hendelse CASCADE;
    TRUNCATE prosessering_task CASCADE;
    TRUNCATE beregningsperiode CASCADE;
    TRUNCATE beregningsgrunnlag CASCADE;
    TRUNCATE vedtaksperioder CASCADE;
    TRUNCATE overstyr_beregning CASCADE;
    TRUNCATE overstyr_beregningsgrunnlag CASCADE;
    TRUNCATE anvendt_trygdetid CASCADE;
    TRUNCATE avkorting_aarsoppgjoer CASCADE;
    TRUNCATE avkorting_aarsoppgjoer_ytelse_foer_avkorting CASCADE;
    TRUNCATE avkorting_aarsoppgjoer_restanse CASCADE;
    TRUNCATE avkortingsgrunnlag_forventet CASCADE;
    TRUNCATE avkortingsgrunnlag_faktisk CASCADE;
    TRUNCATE inntekt_innvilget CASCADE;
    TRUNCATE avkortingsperioder CASCADE;
    TRUNCATE avkortet_ytelse CASCADE;
    TRUNCATE etteroppgjoer_beregnet_resultat CASCADE;
    TRUNCATE sanksjon CASCADE;
    
    ALTER SEQUENCE behandlinghendelse_id_seq RESTART WITH 1;
    ALTER SEQUENCE sak_id_seq RESTART WITH 1;
""",
)
class DatabaseExtension : GenerellDatabaseExtension()
