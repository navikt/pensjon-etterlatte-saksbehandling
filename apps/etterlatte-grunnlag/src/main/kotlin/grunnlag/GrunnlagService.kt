package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
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
    fun hentOpplysningsgrunnlag(sak: Long): Opplysningsgrunnlag
}

class RealGrunnlagService(private val opplysninger: OpplysningDao) : GrunnlagService, VedlikeholdService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentGrunnlag(sak: Long): Grunnlag {
        return opplysninger.finnHendelserIGrunnlag(sak).let { hendelser ->
            Grunnlag(
                saksId = sak,
                grunnlag = hendelser.map { it.opplysning },
                hendelser.maxOfOrNull { it.hendelseNummer } ?: 0
            )
        }
    }

    override fun hentOpplysningsgrunnlag(sak: Long): Opplysningsgrunnlag {
        val opplysninger = opplysninger.finnHendelserIGrunnlag(sak)
        val versjon = opplysninger.maxOfOrNull { it.hendelseNummer } ?: 0

        val map = opplysninger.associateBy({ it.opplysning.opplysningType }, { it.opplysning.toOpplysning() })

        return Opplysningsgrunnlag(
            Grunnlagsdata(søker = map),
            Metadata(sak, versjon)
        )
    }

    override fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstyper): Grunnlagsopplysning<JsonNode>? {
        return opplysninger.finnNyesteGrunnlag(sak, opplysningstype)?.opplysning
    }

    override fun lagreNyeOpplysninger(
        sak: Long,
        fnr: Foedselsnummer?,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    ) {
        logger.info("Oppretter et grunnlag")
        val gjeldendeGrunnlag = opplysninger.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        for (opplysning in nyeOpplysninger) {
            if (opplysning.id in gjeldendeGrunnlag) {
                logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
            } else {
                opplysninger.leggOpplysningTilGrunnlag(sak, opplysning, fnr)
            }
        }
    }

    override fun slettSak(sakId: Long) {
        opplysninger.slettAlleOpplysningerISak(sakId)
    }
}