rootProject.name = "pensjon-etterlatte-saksbehandling"
plugins {
    kotlin("jvm") version "1.9.21" apply false
}
include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-behandling",
    "apps:etterlatte-gyldig-soeknad",
    "apps:etterlatte-opplysninger-fra-soeknad",
    "apps:etterlatte-egne-ansatte-lytter",
    "apps:etterlatte-institusjonsopphold",
    "apps:etterlatte-testdata",
    "apps:etterlatte-utbetaling",
    "apps:etterlatte-oppdater-behandling",
    "apps:etterlatte-beregning",
    "apps:etterlatte-beregning-kafka",
    "apps:etterlatte-vedtaksvurdering",
    "apps:etterlatte-vedtaksvurdering-kafka",
    "apps:etterlatte-grunnlag",
    "apps:etterlatte-brev-api",
    "apps:etterlatte-klage",
    "apps:etterlatte-tilbakekreving",
    "apps:etterlatte-hendelser-pdl",
    "apps:etterlatte-hendelser-joark",
    "apps:etterlatte-hendelser-samordning",
    "apps:etterlatte-migrering",
    "apps:etterlatte-statistikk",
    "apps:etterlatte-vilkaarsvurdering",
    "apps:etterlatte-vilkaarsvurdering-kafka",
    "apps:etterlatte-trygdetid",
    "apps:etterlatte-trygdetid-kafka",
    "apps:etterlatte-samordning-vedtak",
    "libs:ktor2client-auth-clientcredentials",
    "libs:etterlatte-token-model",
    "libs:saksbehandling-common",
    "libs:ktor2client-onbehalfof",
    "libs:rapidsandrivers-extras",
    "libs:etterlatte-behandling-model",
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
    "libs:testdata",
    "libs:etterlatte-trygdetid-model",
    "libs:etterlatte-regler",
    "libs:etterlatte-sporingslogg",
    "libs:etterlatte-utbetaling-model",
    "libs:etterlatte-vedtaksvurdering-model",
    "libs:etterlatte-vilkaarsvurdering-model",
    "libs:etterlatte-tilbakekreving-model",
    "jobs:start-regulering",
    "jobs:start-grunnlagsversjonering",
)
