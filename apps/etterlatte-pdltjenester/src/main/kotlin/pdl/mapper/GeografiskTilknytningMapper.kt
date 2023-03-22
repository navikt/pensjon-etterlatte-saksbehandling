package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.pdl.PdlGeografiskTilknytning
import no.nav.etterlatte.pdl.PdlGtType

object GeografiskTilknytningMapper {
    fun mapGeografiskTilknytning(
        geografiskTilknytning: PdlGeografiskTilknytning
    ): GeografiskTilknytning =
        when (geografiskTilknytning.gtType) {
            PdlGtType.KOMMUNE -> GeografiskTilknytning(kommune = geografiskTilknytning.gtKommune)
            PdlGtType.BYDEL -> GeografiskTilknytning(bydel = geografiskTilknytning.gtBydel)
            PdlGtType.UTLAND -> GeografiskTilknytning(land = geografiskTilknytning.gtLand)
            else -> GeografiskTilknytning()
        }
}