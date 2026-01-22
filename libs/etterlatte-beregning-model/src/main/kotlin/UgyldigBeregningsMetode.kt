package no.nav.etterlatte.libs.common.beregning

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException

class UgyldigBeregningsMetode :
    UgyldigForespoerselException(
        code = "UGYLDIG_BEREGNINGS_METODE",
        detail =
            "Kan ikke ha brukt beregningsmetode 'BEST' i en faktisk beregning, " +
                "siden best velger mellom nasjonal eller prorata når det beregnes.",
    )
