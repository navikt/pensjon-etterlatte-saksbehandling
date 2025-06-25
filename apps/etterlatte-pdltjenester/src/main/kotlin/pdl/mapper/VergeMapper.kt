package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.VergeEllerFullmektig
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.pdl.PdlHentPerson
import no.nav.etterlatte.pdl.PdlVergeEllerFullmektig
import no.nav.etterlatte.pdl.PdlVergemaalEllerFremtidsfullmakt
import java.time.LocalDateTime

object VergeMapper {
    fun mapVerge(hentPerson: PdlHentPerson): List<VergemaalEllerFremtidsfullmakt>? =
        hentPerson.vergemaalEllerFremtidsfullmakt?.mapNotNull(::mapVerge)

    fun mapVerge(vergemaal: PdlVergemaalEllerFremtidsfullmakt): VergemaalEllerFremtidsfullmakt? {
        val opphoerstidspunkt = vergemaal.folkeregistermetadata?.opphoerstidspunkt
        val harOpphoert = opphoerstidspunkt != null && opphoerstidspunkt.isBefore(LocalDateTime.now())
        if (vergemaal.metadata.historisk || harOpphoert) {
            // Vi er ikke interessert i historiske vergemål
            return null
        }
        return toVergemaal(
            embete = vergemaal.embete,
            type = vergemaal.type,
            vergeEllerErFullmektig = vergemaal.vergeEllerFullmektig,
            opphoerstidspunkt = opphoerstidspunkt,
        )
    }

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
        opphoerstidspunkt: LocalDateTime?,
    ) = VergemaalEllerFremtidsfullmakt(
        embete = embete,
        type = type,
        vergeEllerFullmektig = toVergeEllerFullmektig(vergeEllerErFullmektig),
        opphoerstidspunkt = opphoerstidspunkt,
    )
}
