rootProject.name = "pensjon-etterlatte-saksbehandling"

include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-attestering",
    "apps:etterlatte-overvaaking",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-api",
    "apps:etterlatte-behandling",
    "apps:etterlatte-gyldig-soeknad",
    "apps:etterlatte-vilkaar",
    "apps:etterlatte-vilkaar-kafka",
    "apps:etterlatte-opplysninger-fra-soeknad",
    "apps:etterlatte-opplysninger-fra-pdl",
    "apps:etterlatte-testdata",
    "apps:etterlatte-okonomi-vedtak",
    "apps:etterlatte-opplysninger-fra-inntektskomponenten",
    "apps:etterlatte-oppdater-behandling",
    "apps:etterlatte-beregning",
    "libs:ktorclient-auth-clientcredentials",
    "libs:common",
    "libs:ktorclient-onbehalfof",
    "jobs:test-fordeler",
    "apps:etterlatte-vilkaar-api"
)
