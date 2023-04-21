rootProject.name = "pensjon-etterlatte-saksbehandling"
plugins {
    kotlin("jvm") version "1.8.20" apply false
}
include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-behandling",
    "apps:etterlatte-gyldig-soeknad",
    "apps:etterlatte-opplysninger-fra-soeknad",
    "apps:etterlatte-opplysninger-fra-pdl",
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
    "apps:etterlatte-tilbakekreving",
    "apps:etterlatte-hendelser-pdl",
    "apps:etterlatte-statistikk",
    "apps:etterlatte-vilkaarsvurdering",
    "apps:etterlatte-vilkaarsvurdering-kafka",
    "apps:etterlatte-trygdetid",
    "libs:ktor2client-auth-clientcredentials",
    "libs:etterlatte-token-model",
    "libs:saksbehandling-common",
    "libs:ktor2client-onbehalfof",
    "libs:rapidsandrivers-extras",
    "libs:etterlatte-database",
    "libs:etterlatte-funksjonsbrytere",
    "libs:etterlatte-ktor",
    "libs:etterlatte-jobs",
    "libs:etterlatte-kafka",
    "libs:etterlatte-pdl-model",
    "libs:testdata",
    "libs:etterlatte-regler",
    "libs:etterlatte-sporingslogg",
    "jobs:test-fordeler",
    "jobs:start-regulering"
)
include("apps:etterlatte-institusjonsopphold")
findProject(":apps:etterlatte-institusjonsopphold")?.name = "etterlatte-institusjonsopphold"