rootProject.name = "pensjon-etterlatte-saksbehandling"

include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-overvaaking",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-api",
    "apps:etterlatte-behandling",
    "apps:etterlatte-vilkaar",
    "apps:etterlatte-behandling-fra-soeknad",
    "libs:ktorclient-auth-clientcredentials",
    "libs:common",
    "libs:ktorclient-onbehalfof"

)
