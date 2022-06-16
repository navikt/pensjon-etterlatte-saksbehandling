package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.slf4j.LoggerFactory

interface GrunnlagService {
    fun hentGrunnlag(sak: Long): Grunnlag
    fun opprettGrunnlag(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>): Grunnlag
}

class RealGrunnlagService(
    private val opplysninger: OpplysningDao
) : GrunnlagService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentGrunnlag(sak: Long): Grunnlag {
        return inTransaction { hentGrunnlagUtenTransaksjon(sak)}
    }
    override fun opprettGrunnlag(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>): Grunnlag {
        logger.info("Oppretter et grunnlag")
        return inTransaction {
            val gjeldendeGrunnlag = opplysninger.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

            for (opplysning in nyeOpplysninger) {
                if ( opplysning.id in gjeldendeGrunnlag ) {
                    logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
                } else {
                    opplysninger.leggOpplysningTilGrunnlag(sak, opplysning)
                }
            }
            hentGrunnlagUtenTransaksjon(sak)
        }
    }

    private fun hentGrunnlagUtenTransaksjon(sak: Long): Grunnlag {
        return  opplysninger.finnHendelserIGrunnlag(sak).let { hendelser -> Grunnlag(saksId = sak, grunnlag = hendelser.map { it.opplysning }, hendelser.maxOfOrNull { it.hendelseNummer } ?: 0) }
    }
}
