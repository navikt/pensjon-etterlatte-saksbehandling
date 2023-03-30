package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator

class OpplysningsgrunnlagMapper(
    private val grunnlaghendelser: List<OpplysningDao.GrunnlagHendelse>,
    private val sakId: Long,
    private val persongalleri: Persongalleri
) {
    private data class GruppertHendelser(
        val hendelser: List<OpplysningDao.GrunnlagHendelse>
    ) {
        init {
            assert(alleOpplysningerErAvSammeType(hendelser.map { it.opplysning }))
        }

        val opplysning: Opplysning<JsonNode> = if (hendelser.any { it.opplysning.periode != null }) {
            Opplysning.Periodisert.create(hendelser.map { it.opplysning })
        } else {
            hendelser.maxBy { hendelse -> hendelse.hendelseNummer }
                .let { Opplysning.Konstant.create(it.opplysning) }
        }

        val opplysningstype: Opplysningstype
            get() = hendelser.first().opplysning.opplysningType
        val fnr: Folkeregisteridentifikator?
            get() = hendelser.first().opplysning.fnr

        private fun alleOpplysningerErAvSammeType(opplysninger: List<Grunnlagsopplysning<*>>): Boolean {
            return (opplysninger.size == opplysninger.filter { erPeriodisertOpplysning(it) }.size) ||
                (opplysninger.size == opplysninger.filterNot { erPeriodisertOpplysning(it) }.size)
        }

        private fun erPeriodisertOpplysning(grunnlagsopplysning: Grunnlagsopplysning<*>) =
            grunnlagsopplysning.periode != null
    }

    fun hentGrunnlag(): Grunnlag {
        val grupper = grunnlaghendelser.groupByFnrAndOpplysningstype()
        val senestVersjon = grupper.hentSenesteHendelserPerGruppe().maxOfOrNull { it.hendelseNummer } ?: 0
        val opplysninger = grupper.map { GruppertHendelser(it) }

        val (personopplysninger, saksopplysninger) = opplysninger.partition { it.fnr !== null }
        val (søker, familie) = personopplysninger.partition { it.fnr!!.value == persongalleri.soeker }

        val søkerMap = søker.associateBy({ it.opplysningstype }, { it.opplysning })
        val familieMap = familie
            .groupBy { it.fnr }.values
            .map { familiemedlem ->
                familiemedlem.associateBy({ it.opplysningstype }, { it.opplysning })
            }
        val sakMap = saksopplysninger.associateBy({ it.opplysningstype }, { it.opplysning })

        return Grunnlag(
            soeker = søkerMap,
            familie = familieMap,
            sak = sakMap,
            metadata = Metadata(sakId, senestVersjon)
        )
    }

    private fun List<OpplysningDao.GrunnlagHendelse>.groupByFnrAndOpplysningstype() =
        this.groupBy { Pair(it.opplysning.fnr, it.opplysning.opplysningType) }.values

    private fun Collection<List<OpplysningDao.GrunnlagHendelse>>.hentSenesteHendelserPerGruppe() =
        this.map { it.maxBy { hendelse -> hendelse.hendelseNummer } }
}