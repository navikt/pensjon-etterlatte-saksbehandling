package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.PeriodisertOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.person.Foedselsnummer

class OpplysningsgrunnlagMapper(
    private val grunnlaghendelser: List<OpplysningDao.GrunnlagHendelse>,
    private val sakId: Long,
    private val persongalleri: Persongalleri
) {
    private data class GruppertHendelser(
        val hendelser: List<OpplysningDao.GrunnlagHendelse>
    ) {
        init {
            assert(alleOpplysingerHarSammeType(opplysninger))
        }

        private val opplysninger
            get() = hendelser.map { it.opplysning.toOpplysning() }

        val opplysning: Opplysning<JsonNode> = hendelser.first().let {
            when (it.opplysning.toOpplysning()) {
                is Opplysning.Konstant -> hendelser.maxBy { hendelse -> hendelse.hendelseNummer }
                    .opplysning.toOpplysning()

                is Opplysning.Periodisert -> Opplysning.Periodisert(
                    hendelser.map { hendelse ->
                        PeriodisertOpplysning(
                            hendelse.opplysning.id,
                            kilde = hendelse.opplysning.kilde,
                            verdi = hendelse.opplysning.opplysning,
                            fom = hendelse.opplysning.periode!!.fom,
                            tom = hendelse.opplysning.periode!!.tom
                        )
                    }
                )
            }
        }

        val opplysningstype: Opplysningstyper
            get() = hendelser.first().opplysning.opplysningType
        val fnr: Foedselsnummer?
            get() = hendelser.first().opplysning.fnr

        private fun alleOpplysingerHarSammeType(opplysninger: List<Opplysning<*>>): Boolean {
            return (opplysninger.size == opplysninger.filterIsInstance<Opplysning.Konstant<*>>().size) ||
                (opplysninger.size == opplysninger.filterIsInstance<Opplysning.Periodisert<*>>().size)
        }
    }

    fun hentOpplysningsgrunnlag(): Opplysningsgrunnlag {
        val grupper = grunnlaghendelser.groupByFnrAndOpplysningstype()
        val senestVersjon = grupper.hentSenesteHendelserPerGruppe().maxOfOrNull { it.hendelseNummer } ?: 0
        val opplysninger = grupper.map { GruppertHendelser(it) }

        val (personopplysninger, saksopplysninger) = opplysninger.partition { it.fnr !== null }
        val (søker, familie) = personopplysninger.partition { it.fnr!!.value == persongalleri.soeker }

        /* TODO ai: håndter periodisert vs konstant */
        val søkerMap = søker.associateBy({ it.opplysningstype }, { it.opplysning })
        val familieMap = familie
            .groupBy { it.fnr }.values
            .map { familiemedlem ->
                familiemedlem.associateBy({ it.opplysningstype }, { it.opplysning })
            }
        val sakMap = saksopplysninger.associateBy({ it.opplysningstype }, { it.opplysning })

        return Opplysningsgrunnlag(
            søker = søkerMap,
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