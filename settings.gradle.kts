rootProject.name = "pensjon-etterlatte-saksbehandling"
include(
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-behandling",
    "apps:etterlatte-egne-ansatte-lytter",
    "apps:etterlatte-institusjonsopphold",
    "apps:etterlatte-testdata",
    "apps:etterlatte-testdata-behandler",
    "apps:etterlatte-utbetaling",
    "apps:etterlatte-behandling-kafka",
    "apps:etterlatte-beregning",
    "apps:etterlatte-beregning-kafka",
    "apps:etterlatte-vedtaksvurdering",
    "apps:etterlatte-vedtaksvurdering-kafka",
    "apps:etterlatte-grunnlag",
    "apps:etterlatte-brev-api",
    "apps:etterlatte-brev-kafka",
    "apps:etterlatte-klage",
    "apps:etterlatte-tilbakekreving",
    "apps:etterlatte-hendelser-pdl",
    "apps:etterlatte-hendelser-joark",
    "apps:etterlatte-hendelser-samordning",
    "apps:etterlatte-hendelser-ufoere",
    "apps:etterlatte-migrering",
    "apps:etterlatte-statistikk",
    "apps:etterlatte-tidshendelser",
    "apps:etterlatte-trygdetid",
    "apps:etterlatte-trygdetid-kafka",
    "apps:etterlatte-api",
    "libs:saksbehandling-common",
    "libs:rapidsandrivers-extras",
    "libs:etterlatte-behandling-model",
    "libs:etterlatte-brev-model",
    "libs:etterlatte-oppgave-model",
    "libs:etterlatte-beregning-model",
    "libs:etterlatte-database",
    "libs:etterlatte-funksjonsbrytere",
    "libs:etterlatte-ktor",
    "libs:etterlatte-jobs",
    "libs:etterlatte-kafka",
    "libs:etterlatte-migrering-model",
    "libs:etterlatte-mq",
    "libs:etterlatte-pdl-model",
    "libs:etterlatte-institusjonsopphold-model",
    "libs:etterlatte-trygdetid-model",
    "libs:etterlatte-regler",
    "libs:etterlatte-sporingslogg",
    "libs:etterlatte-utbetaling-model",
    "libs:etterlatte-vedtaksvurdering-model",
    "libs:etterlatte-vilkaarsvurdering-model",
    "libs:etterlatte-tilbakekreving-model",
    "libs:etterlatte-tidshendelser-model",
    "libs:etterlatte-omregning-model",
    "libs:etterlatte-inntektsjustering-model",
)
