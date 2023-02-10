rootProject.name = "pensjon-etterlatte-saksbehandling"
plugins {
    kotlin("jvm") version "1.8.10" apply false
}
include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-behandling",
    "apps:etterlatte-brreg",
    "apps:etterlatte-gyldig-soeknad",
    "apps:etterlatte-opplysninger-fra-soeknad",
    "apps:etterlatte-opplysninger-fra-pdl",
    "apps:etterlatte-testdata",
    "apps:etterlatte-utbetaling",
    "apps:etterlatte-opplysninger-fra-inntektskomponenten",
    "apps:etterlatte-oppdater-behandling",
    "apps:etterlatte-beregning",
    "apps:etterlatte-vedtaksvurdering",
    "apps:etterlatte-grunnlag",
    "apps:etterlatte-brev-api",
    "apps:etterlatte-tilbakekreving",
    "apps:etterlatte-hendelser-pdl",
    "apps:etterlatte-statistikk",
    "apps:etterlatte-vilkaarsvurdering",
    "libs:ktor2client-auth-clientcredentials",
    "libs:common",
    "libs:ktor2client-onbehalfof",
    "libs:rapidsandrivers-extras",
    "libs:etterlatte-database",
    "libs:etterlatte-ktor",
    "libs:etterlatte-jobs",
    "libs:etterlatte-kafka",
    "libs:testdata",
    "libs:etterlatte-regler",
    "libs:etterlatte-sporingslogg",
    "libs:etterlatte-helsesjekk",
    "jobs:test-fordeler"
)