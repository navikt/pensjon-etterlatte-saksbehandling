package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import org.slf4j.LoggerFactory
import rapidsandrivers.vedlikehold.VedlikeholdService

interface GrunnlagService {
    fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>?
    fun lagreNyeOpplysninger(sak: Long, fnr: Foedselsnummer?, nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>)
    fun hentOpplysningsgrunnlag(sak: Long, persongalleri: Persongalleri): Grunnlag
}

class RealGrunnlagService(private val opplysningDao: OpplysningDao) : GrunnlagService, VedlikeholdService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentOpplysningsgrunnlag(sak: Long, persongalleri: Persongalleri): Grunnlag {
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sak)

        return OpplysningsgrunnlagMapper(grunnlag, sak, persongalleri).hentOpplysningsgrunnlag()
    }

    override fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>? {
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