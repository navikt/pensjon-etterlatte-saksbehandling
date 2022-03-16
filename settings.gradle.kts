rootProject.name = "pensjon-etterlatte-saksbehandling"

include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-overvaaking",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-api",
    "apps:etterlatte-behandling",
    "apps:etterlatte-vilkaar",
    "apps:etterlatte-behandling-fra-soeknad",
    "apps:etterlatte-opplysninger-fra-pdl",
    "apps:etterlatte-testdata",
    "apps:etterlatte-okonomi-vedtak",
    "libs:ktorclient-auth-clientcredentials",
    "libs:common",
    "libs:ktorclient-onbehalfof",
    "jobs:test-fordeler"
)
