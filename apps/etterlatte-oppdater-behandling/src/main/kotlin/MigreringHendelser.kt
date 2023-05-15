package no.nav.etterlatte

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.rapidsandrivers.migrering.FULLSTENDIG_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.GRUNNLAG_OPPDATERT
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.OPPLYSNING_KEY
import rapidsandrivers.withFeilhaandtering
import java.util.*

internal class MigreringHendelser(rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        logger.info("initierer rapid for migreringshendelser")
        River(rapidsConnection).apply {
            eventName(GRUNNLAG_OPPDATERT)

            correlationId()
            validate { it.requireValue(BEHOV_NAME_KEY, Opplysningstype.MIGRERING.name) }
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
            validate { it.requireKey(OPPLYSNING_KEY) }

            validate { it.rejectKey(FULLSTENDIG_KEY) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            withFeilhaandtering(packet, context, GRUNNLAG_OPPDATERT) {
                logger.info("Mottatt migreringshendelse, klar til å opprette persongalleri")
                val request = objectMapper.treeToValue(packet[HENDELSE_DATA_KEY], MigreringRequest::class.java)
                packet[OPPLYSNING_KEY] = tilOpplysning(request.persongalleri)
                packet.eventName = "OPPLYSNING:NY"
                packet[FULLSTENDIG_KEY] = true
                context.publish(packet.toJson())
                logger.info("Publiserte persongalleri")
            }
        }
    }

    private fun tilOpplysning(persongalleri: Persongalleri) =
        listOf(
            Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Pesys.create(),
                Opplysningstype.PERSONGALLERI_V1,
                objectMapper.createObjectNode(),
                persongalleri
            )
        )
}