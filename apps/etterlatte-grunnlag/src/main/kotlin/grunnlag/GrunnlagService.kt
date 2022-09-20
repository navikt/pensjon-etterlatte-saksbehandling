package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Metadata
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory
import rapidsandrivers.vedlikehold.VedlikeholdService

interface GrunnlagService {
    fun hentGrunnlag(sak: Long): Grunnlag
    fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstyper): Grunnlagsopplysning<JsonNode>?
    fun lagreNyeOpplysninger(sak: Long, fnr: Foedselsnummer?, nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>)
    fun hentOpplysningsgrunnlag(sak: Long, persongalleri: Persongalleri): Opplysningsgrunnlag
}

class RealGrunnlagService(private val opplysningDao: OpplysningDao) : GrunnlagService, VedlikeholdService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentGrunnlag(sak: Long): Grunnlag {
        return opplysningDao.finnHendelserIGrunnlag(sak).let { hendelser ->
            Grunnlag(
                saksId = sak,
                grunnlag = hendelser.map { it.opplysning },
                hendelser.maxOfOrNull { it.hendelseNummer } ?: 0
            )
        }
    }

    private fun List<OpplysningDao.GrunnlagHendelse>.groupByFnrAndOpplysningstype() =
        this.groupBy { Pair(it.opplysning.fnr, it.opplysning.opplysningType) }.values

    private fun Collection<List<OpplysningDao.GrunnlagHendelse>>.hentSenesteOpplysningerPerGruppe() =
        this.map { it.maxBy { hendelse -> hendelse.hendelseNummer } }

    override fun hentOpplysningsgrunnlag(sak: Long, persongalleri: Persongalleri): Opplysningsgrunnlag {
        val opplysninger = opplysningDao
            .hentAlleGrunnlagForSak(sak)
            .groupByFnrAndOpplysningstype()
            .hentSenesteOpplysningerPerGruppe()

        val versjon = opplysninger.maxOfOrNull { it.hendelseNummer } ?: 0
        val (personopplysninger, saksopplysninger) = opplysninger.partition { it.opplysning.fnr !== null }

        val (søker, familie) = personopplysninger.partition { it.opplysning.fnr!!.value == persongalleri.soeker }

        val søkerMap = søker.associateBy({ it.opplysning.opplysningType }, { it.opplysning.toOpplysning() })
        val familieMap = familie
            .groupBy { it.opplysning.fnr }.values
            .map { familiemedlem ->
                familiemedlem.associateBy({ it.opplysning.opplysningType }, { it.opplysning.toOpplysning() })
            }
        val sakMap = saksopplysninger.associateBy({ it.opplysning.opplysningType }, { it.opplysning.toOpplysning() })

        return Opplysningsgrunnlag(
            søker = søkerMap,
            familie = familieMap,
            sak = sakMap,
            metadata = Metadata(sak, versjon)
        )
    }

    override fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstyper): Grunnlagsopplysning<JsonNode>? {
        return opplysningDao.finnNyesteGrunnlag(sak, opplysningstype)?.opplysning
    }

    override fun lagreNyeOpplysninger(
        sak: Long,
        fnr: Foedselsnummer?,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    ) {
        logger.info("Oppretter et grunnlag")
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        for (opplysning in nyeOpplysninger) {
            if (opplysning.id in gjeldendeGrunnlag) {
                logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
            } else {
                opplysningDao.leggOpplysningTilGrunnlag(sak, opplysning, fnr)
            }
        }
    }

    override fun slettSak(sakId: Long) {
        opplysningDao.slettAlleOpplysningerISak(sakId)
    }
}