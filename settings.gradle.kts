rootProject.name = "pensjon-etterlatte-saksbehandling"
plugins {
    kotlin("jvm") version "1.7.0" apply false
}
include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-overvaaking",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-api",
    "apps:etterlatte-behandling",
    "apps:etterlatte-brreg",
    "apps:etterlatte-medl-proxy",
    "apps:etterlatte-gyldig-soeknad",
    "apps:etterlatte-vilkaar-kafka",
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
    "apps:etterlatte-brev-distribusjon",
    "apps:etterlatte-avkorting",
    "apps:etterlatte-tilbakekreving",
    "apps:etterlatte-hendelser-pdl",
    "libs:ktor2client-auth-clientcredentials",
    "libs:common",
    "libs:ktor2client-onbehalfof",
    "libs:rapidsandrivers-extras",
    "libs:etterlatte-kafka",
    "jobs:test-fordeler"
)