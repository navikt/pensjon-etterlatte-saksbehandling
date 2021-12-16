rootProject.name = "pensjon-etterlatte-saksbehandling"

include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-overvaaking",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-api",
    "apps:etterlatte-behandling",
    "apps:etterlatte-vilkaar",
    "libs:ktorclient-auth-clientcredentials",
    "libs:common"
)
