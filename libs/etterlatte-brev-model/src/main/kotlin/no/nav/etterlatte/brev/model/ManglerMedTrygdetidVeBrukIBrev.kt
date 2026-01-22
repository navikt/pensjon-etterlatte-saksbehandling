package no.nav.etterlatte.brev.model

import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException

// Brukes der mangler med trygdetid ikke skal kunne skje men felt likevel er nullable
class ManglerMedTrygdetidVeBrukIBrev :
    UgyldigForespoerselException(
        code = "MANGLER_TRYGDETID_VED_BREV",
        detail = "Trygdetid har mangler ved bruk til brev",
    )
