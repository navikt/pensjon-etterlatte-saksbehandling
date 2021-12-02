rootProject.name = "pensjon-etterlatte-saksbehandling"

include(
    "apps:etterlatte-fordeler",
    "apps:etterlatte-overvaaking",
    "apps:etterlatte-pdltjenester",
    "apps:etterlatte-api",
    "apps:etterlatte-behandling",
    "libs:ktorclient-auth-clientcredentials",
    "libs:common"
)
