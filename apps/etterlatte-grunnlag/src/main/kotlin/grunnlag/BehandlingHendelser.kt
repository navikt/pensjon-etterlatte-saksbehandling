package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.behovNameKey
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class BehandlingHendelser(
    rapidsConnection: RapidsConnection
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:OPPRETTET")
            correlationId()
            validate { it.requireKey("persongalleri") }
            validate { it.requireKey("sakId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            // TODO dette må jeg gjøre smartere, ellers må Persongalleri restruktureres
            context.publish(
                JsonMessage.newMessage(
                    mapOf(
                        behovNameKey to Opplysningstype.SOEKER_PDL_V1,
                        "sakId" to packet["sakId"],
                        "fnr" to packet["persongalleri"]["soeker"],
                        "rolle" to PersonRolle.BARN,
                        correlationIdKey to packet[correlationIdKey]
                    )
                ).toJson()
            )

            packet["persongalleri"]["gjenlevende"].forEach { fnr ->
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            behovNameKey to Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                            "sakId" to packet["sakId"],
                            "fnr" to fnr.asText(),
                            "rolle" to PersonRolle.GJENLEVENDE,
                            correlationIdKey to packet[correlationIdKey]
                        )
                    ).toJson()
                )
            }

            packet["persongalleri"]["avdoed"].forEach { fnr ->
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            behovNameKey to Opplysningstype.AVDOED_PDL_V1,
                            "sakId" to packet["sakId"],
                            "fnr" to fnr.asText(),
                            "rolle" to PersonRolle.AVDOED,
                            correlationIdKey to packet[correlationIdKey]
                        )
                    ).toJson()
                )
            }
        }
}