package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.MigrerSoekerRequest
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.rapidsandrivers.migrering.MIGRERING_GRUNNLAG_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser.BEHANDLING_OPPRETTET
import no.nav.etterlatte.rapidsandrivers.migrering.PERSONGALLERI
import no.nav.etterlatte.rapidsandrivers.migrering.persongalleri
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.SAK_ID_KEY
import rapidsandrivers.sakId
import rapidsandrivers.withFeilhaandtering
import java.util.*

class MigreringHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService
) : River.PacketListener {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    init {
        River(rapidsConnection).apply {
            correlationId()
            eventName(BEHANDLING_OPPRETTET)
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(MIGRERING_GRUNNLAG_KEY) }
            validate { it.requireKey(PERSONGALLERI) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, BEHANDLING_OPPRETTET) {
                logger.info("Mottok grunnlagshendelser for migrering")
                val sakId = packet.sakId
                val request =
                    objectMapper.treeToValue(packet[MIGRERING_GRUNNLAG_KEY], MigrerSoekerRequest::class.java)

                lagreEnkeltgrunnlag(
                    sakId,
                    listOf(
                        Grunnlagsopplysning(
                            UUID.randomUUID(),
                            Grunnlagsopplysning.Pesys.create(),
                            Opplysningstype.PERSONGALLERI_V1,
                            objectMapper.createObjectNode(),
                            packet.persongalleri.toJsonNode()
                        )
                    ),
                    request.soeker
                )

                packet.eventName = Migreringshendelser.VILKAARSVURDER
                context.publish(packet.toJson())

                logger.info("Behandla grunnlagshendelser for migrering for sak $sakId")
            }
        }
    }

    private fun lagreEnkeltgrunnlag(
        sakId: Long,
        opplysninger: List<Grunnlagsopplysning<JsonNode>>,
        fnr: String
    ) =
        grunnlagService.lagreNyePersonopplysninger(
            sakId,
            Folkeregisteridentifikator.of(fnr),
            opplysninger
        )
}