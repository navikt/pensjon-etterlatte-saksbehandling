package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.pdl.PdlVergeEllerFullmektig
import no.nav.etterlatte.pdl.PdlVergemaalEllerFremtidsfullmakt

object VergeMapper {
    fun mapVerge(vergemaalsListe: List<PdlVergemaalEllerFremtidsfullmakt>): List<VergemaalEllerFremtidsfullmakt> =
        vergemaalsListe.map {
            fun toVergeEllerFullmektig(pdlV: PdlVergeEllerFullmektig) =
                VergeEllerFullmektig(
                    motpartsPersonident = pdlV.motpartsPersonident,
                    navn =
                        listOfNotNull(pdlV.navn?.fornavn, pdlV.navn?.mellomnavn, pdlV.navn?.etternavn)
                            .joinToString(" "),
                    tjenesteomraade = pdlV.tjenesteomraade?.joinToString(","),
                    omfangetErInnenPersonligOmraade = pdlV.omfangetErInnenPersonligOmraade,
                )

            fun toVergemaal(
                embete: String? = null,
                type: String?,
                vergeEllerErFullmektig: PdlVergeEllerFullmektig,
            ) = VergemaalEllerFremtidsfullmakt(
                embete = embete,
                type = type,
                vergeEllerFullmektig = toVergeEllerFullmektig(vergeEllerErFullmektig),
            )

            toVergemaal(
                embete = it.embete,
                type = it.type,
                vergeEllerErFullmektig = it.vergeEllerFullmektig,
            )
        }
}
