package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.JsonMessage.Companion.newMessage
import no.nav.helse.rapids_rivers.MessageContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

interface GrunnlagService {
    fun hentGrunnlag(sak: Long): Grunnlag
    fun opprettGrunnlag(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>): Grunnlag
    fun opplysningFraSaksbehandler(sak: Long, saksbehandlerOpplysninger: List<Grunnlagsopplysning<ObjectNode>>)
}

class RealGrunnlagService(
    private val opplysninger: OpplysningDao,
    private val rapid: AtomicReference<MessageContext> = AtomicReference()
) : GrunnlagService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentGrunnlag(sak: Long): Grunnlag {
        return inTransaction { hentGrunnlagUtenTransaksjon(sak) }
    }

    override fun opprettGrunnlag(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<ObjectNode>>): Grunnlag {
        logger.info("Oppretter et grunnlag")
        return inTransaction {
            val gjeldendeGrunnlag = opplysninger.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

            for (opplysning in nyeOpplysninger) {
                if (opplysning.id in gjeldendeGrunnlag) {
                    logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
                } else {
                    opplysninger.leggOpplysningTilGrunnlag(sak, opplysning)
                }
            }
            hentGrunnlagUtenTransaksjon(sak)
        }
    }

    override fun opplysningFraSaksbehandler(sak: Long, saksbehandlerOpplysninger: List<Grunnlagsopplysning<ObjectNode>>) {
        rapid.get().publish(
            sak.toString(),
            newMessage(
                mapOf(
                    "sak" to sak,
                    "opplysning" to saksbehandlerOpplysninger
                )
            ).toJson()
        )
    }

    private fun hentGrunnlagUtenTransaksjon(sak: Long): Grunnlag {
        return opplysninger.finnHendelserIGrunnlag(sak).let { hendelser ->
            Grunnlag(saksId = sak, grunnlag = hendelser.map { it.opplysning }, hendelser.maxOfOrNull { it.hendelseNummer }
                ?: 0)
        }
    }
}
