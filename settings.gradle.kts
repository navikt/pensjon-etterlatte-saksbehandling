rootProject.name = "pensjon-etterlatte-saksbehandling"

include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-attestering",
    "apps:etterlatte-overvaaking",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-api",
    "apps:etterlatte-behandling",
    "apps:etterlatte-vilkaar",
    "apps:etterlatte-vilkaar-kafka",
    "apps:etterlatte-behandling-fra-soeknad",
    "apps:etterlatte-opplysninger-fra-pdl",
    "apps:etterlatte-testdata",
    "apps:etterlatte-okonomi-vedtak",
    "apps:etterlatte-opplysninger-fra-inntektskomponenten",
    "libs:ktorclient-auth-clientcredentials",
    "libs:common",
    "libs:ktorclient-onbehalfof",
    "jobs:test-fordeler"
)
