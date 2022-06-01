rootProject.name = "pensjon-etterlatte-saksbehandling"

include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-attestering",
    "apps:etterlatte-overvaaking",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-api",
    "apps:etterlatte-behandling",
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
    "apps:etterlatte-avkorting",
    "libs:ktorclient-auth-clientcredentials",
    "libs:common",
    "libs:ktorclient-onbehalfof",
    "jobs:test-fordeler",
)
