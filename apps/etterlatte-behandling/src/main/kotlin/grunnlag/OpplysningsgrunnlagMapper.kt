package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

class OpplysningsgrunnlagMapper(
    private val grunnlaghendelser: List<OpplysningDao.GrunnlagHendelse>,
    private val persongalleri: Persongalleri,
) {
    private data class GruppertHendelser(
        val hendelser: List<OpplysningDao.GrunnlagHendelse>,
    ) {
        val opplysning: Opplysning<JsonNode> =
            hendelser
                .maxBy { hendelse -> hendelse.hendelseNummer }
                .let { Opplysning.Konstant.create(it.opplysning) }

        val opplysningstype: Opplysningstype
            get() = hendelser.first().opplysning.opplysningType
        val fnr: Folkeregisteridentifikator?
            get() = hendelser.first().opplysning.fnr
    }

    fun hentGrunnlag(): Grunnlag {
        val grupper = grunnlaghendelser.groupByFnrAndOpplysningstype()
        val senestVersjon = grupper.hentSenesteHendelserPerGruppe().maxOfOrNull { it.hendelseNummer } ?: 0
        val opplysninger = grupper.map { GruppertHendelser(it) }

        val (personopplysninger, saksopplysninger) = opplysninger.partition { it.fnr !== null }
        val (soeker, familie) = personopplysninger.partition { it.fnr!!.value == persongalleri.soeker }

        val soekerMap = soeker.associateBy({ it.opplysningstype }, { it.opplysning })
        val familieMap =
            familie
                .groupBy { it.fnr }
                .values
                .map { familiemedlem ->
                    familiemedlem.associateBy({ it.opplysningstype }, { it.opplysning })
                }
        val sakMap = saksopplysninger.associateBy({ it.opplysningstype }, { it.opplysning })

        val sakId = grunnlaghendelser.first().sakId

        return Grunnlag(
            soeker = soekerMap,
            familie = familieMap,
            sak = sakMap,
            metadata = Metadata(sakId, senestVersjon),
        )
    }

    private fun List<OpplysningDao.GrunnlagHendelse>.groupByFnrAndOpplysningstype() =
        this.groupBy { Pair(it.opplysning.fnr, it.opplysning.opplysningType) }.values

    private fun Collection<List<OpplysningDao.GrunnlagHendelse>>.hentSenesteHendelserPerGruppe() =
        this.map { it.maxBy { hendelse -> hendelse.hendelseNummer } }
}
