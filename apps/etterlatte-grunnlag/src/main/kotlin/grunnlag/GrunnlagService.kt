package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.toJson
import org.slf4j.LoggerFactory
import rapidsandrivers.vedlikehold.VedlikeholdService

interface GrunnlagService {
    fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>?
    fun lagreNyeOpplysninger(sak: Long, fnr: Foedselsnummer?, nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>)
    fun hentOpplysningsgrunnlag(sak: Long): Grunnlag?
    fun hentOpplysningsgrunnlagMedVersjon(sak: Long, versjon: Long): Grunnlag?
    fun hentOpplysningsgrunnlag(
        sak: Long,
        persongalleri: Persongalleri
    ): Grunnlag // TODO ai: Kan fjernes når kafka flyten fjernes
}

class RealGrunnlagService(private val opplysningDao: OpplysningDao) : GrunnlagService, VedlikeholdService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentOpplysningsgrunnlag(sak: Long): Grunnlag? {
        val persongalleriJsonNode = opplysningDao.finnNyesteGrunnlag(sak, Opplysningstype.PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for sak $sak. Fant ikke persongalleri")
            return null
        }
        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sak)

        return OpplysningsgrunnlagMapper(grunnlag, sak, persongalleri).hentGrunnlag()
    }

    override fun hentOpplysningsgrunnlag(sak: Long, persongalleri: Persongalleri): Grunnlag {
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sak)

        return OpplysningsgrunnlagMapper(grunnlag, sak, persongalleri).hentGrunnlag()
    }

    override fun hentOpplysningsgrunnlagMedVersjon(sak: Long, versjon: Long): Grunnlag? {
        val grunnlag = opplysningDao.finnGrunnlagOpptilVersjon(sak, versjon)

        val personGalleri = grunnlag.find { it.opplysning.opplysningType === Opplysningstype.PERSONGALLERI_V1 }
            ?.let { objectMapper.readValue(it.opplysning.opplysning.toJson(), Persongalleri::class.java) }
            ?: run {
                logger.info(
                    "Klarte ikke å hente ut grunnlag for sak $sak med versjon $versjon. " +
                        "Fant ikke persongalleri"
                )
                return null
            }

        return OpplysningsgrunnlagMapper(grunnlag, sak, personGalleri).hentGrunnlag()
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