package no.nav.etterlatte.grunnlag

import MigreringGrunnlagRequest
import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_GRUNNLAG_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.PERSONGALLERI_GRUNNLAG
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering

class MigreringHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            correlationId()
            eventName(PERSONGALLERI_GRUNNLAG)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(MIGRERING_GRUNNLAG_KEY) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, PERSONGALLERI_GRUNNLAG) {
                logger.info("Mottok grunnlagshendelser for migrering")
                val sakId = packet.sakId
                val request =
                    objectMapper.treeToValue(packet[MIGRERING_GRUNNLAG_KEY], MigreringGrunnlagRequest::class.java)

                lagreEnkeltgrunnlag(sakId, request.soeker.second, request.soeker.first)
                request.gjenlevende.forEach { lagreEnkeltgrunnlag(sakId, it.second, it.first) }
                request.avdoede.forEach { lagreEnkeltgrunnlag(sakId, it.second, it.first) }

                packet.eventName = Migreringshendelser.VILKAARSVURDER
                context.publish(packet.toJson())

                logger.info("Behandla grunnlagshendelser for migrering for sak $sakId")
            }
        }
    }

    private fun lagreEnkeltgrunnlag(
        sakId: Long,
        opplysninger: List<Grunnlagsopplysning<JsonNode>>,

        fnr: String?
    ) = if (fnr == null) {
        grunnlagService.lagreNyeSaksopplysninger(
            sakId,
            opplysninger
        )
    } else {
        grunnlagService.lagreNyePersonopplysninger(
            sakId,
            Folkeregisteridentifikator.of(fnr),
            opplysninger
        )
    }
}