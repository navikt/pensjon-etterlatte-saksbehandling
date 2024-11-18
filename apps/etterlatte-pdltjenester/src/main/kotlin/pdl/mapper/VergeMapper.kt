package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlVergeEllerFullmektig
import no.nav.etterlatte.pdl.PdlVergemaalEllerFremtidsfullmakt

object VergeMapper {
    fun mapVerge(hentPerson: PdlHentPerson): List<VergemaalEllerFremtidsfullmakt>? =
        hentPerson.vergemaalEllerFremtidsfullmakt?.map(::mapVerge)

    fun mapVerge(vergemaal: PdlVergemaalEllerFremtidsfullmakt): VergemaalEllerFremtidsfullmakt =
        toVergemaal(
            embete = vergemaal.embete,
            type = vergemaal.type,
            vergeEllerErFullmektig = vergemaal.vergeEllerFullmektig,
        )

    private fun toVergeEllerFullmektig(pdlV: PdlVergeEllerFullmektig) =
        VergeEllerFullmektig(
            motpartsPersonident = pdlV.motpartsPersonident,
            navn =
                listOfNotNull(pdlV.navn?.fornavn, pdlV.navn?.mellomnavn, pdlV.navn?.etternavn)
                    .joinToString(" "),
            tjenesteomraade = pdlV.tjenesteomraade?.joinToString(","),
            omfangetErInnenPersonligOmraade = pdlV.omfangetErInnenPersonligOmraade,
        )

    private fun toVergemaal(
        embete: String? = null,
        type: String?,
        vergeEllerErFullmektig: PdlVergeEllerFullmektig,
    ) = VergemaalEllerFremtidsfullmakt(
        embete = embete,
        type = type,
        vergeEllerFullmektig = toVergeEllerFullmektig(vergeEllerErFullmektig),
    )
}
