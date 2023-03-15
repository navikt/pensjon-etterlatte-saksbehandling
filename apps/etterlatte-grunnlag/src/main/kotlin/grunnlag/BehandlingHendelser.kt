package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.libs.common.event.BehandlingRiverKey
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class BehandlingHendelser(
    rapidsConnection: RapidsConnection,
    private val grunnlagService: GrunnlagService
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GYLDIG_FREMSATT")
            correlationId()
            validate { it.requireKey(BehandlingRiverKey.sakIdKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val sakId = packet["sakId"].asLong()

            val persongalleriJsonNode =
                requireNotNull(grunnlagService.hentGrunnlagAvType(sakId, Opplysningstype.PERSONGALLERI_V1)) {
                    "Persongalleri ikke funnet for sakid=$sakId"
                }.opplysning

            context.publish(
                JsonMessage.newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.SOEKER_PDL_V1,
                        "sakId" to sakId,
                        "fnr" to persongalleriJsonNode["soeker"],
                        "rolle" to PersonRolle.BARN,
                        CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                    )
                ).toJson()
            )

            persongalleriJsonNode["gjenlevende"].forEach { fnr ->
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            BEHOV_NAME_KEY to Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                            "sakId" to sakId,
                            "fnr" to fnr.asText(),
                            "rolle" to PersonRolle.GJENLEVENDE,
                            CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                        )
                    ).toJson()
                )
            }

            persongalleriJsonNode["avdoed"].forEach { fnr ->
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            BEHOV_NAME_KEY to Opplysningstype.AVDOED_PDL_V1,
                            "sakId" to sakId,
                            "fnr" to fnr.asText(),
                            "rolle" to PersonRolle.AVDOED,
                            CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                        )
                    ).toJson()
                )
            }
        }
}