package no.nav.etterlatte.grunnlag

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BehandlingHendelser(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {

    private val logger: Logger = LoggerFactory.getLogger(GrunnlagHendelser::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:OPPRETTET")
            correlationId()
            validate { it.requireKey("persongalleri") }
            validate { it.requireKey("sak") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            if (Kontekst.get().AppUser !is Self) {
                logger.warn("AppUser i kontekst er ikke Self i R&R-flyten")
            }

            //TODO dette må jeg gjøre smartere, ellers må Persongalleri restruktureres
            context.publish(
                JsonMessage.newMessage(
                    mapOf(
                        behovNameKey to Opplysningstyper.SOEKER_PDL_V1,
                        "sakId" to packet["sak"],
                        "fnr" to packet["persongalleri.soeker"],
                        "rolle" to Opplysningstyper.SOEKER_PDL_V1.personRolle!!,
                        correlationIdKey to packet[correlationIdKey]
                    )
                ).toJson())

            packet["persongalleri.gjenlevende"].forEach { fnr ->
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            behovNameKey to Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1,
                            "sakId" to packet["sak"],
                            "fnr" to fnr.asText(),
                            "rolle" to Opplysningstyper.SOEKER_PDL_V1.personRolle!!,
                            correlationIdKey to packet[correlationIdKey]
                        )
                    ).toJson()
                )
            }

            packet["persongalleri.avdoed"].forEach { fnr ->
                context.publish(
                    JsonMessage.newMessage(
                        mapOf(
                            behovNameKey to Opplysningstyper.AVDOED_PDL_V1,
                            "sakId" to packet["sak"],
                            "fnr" to fnr.asText(),
                            "rolle" to Opplysningstyper.AVDOED_PDL_V1.personRolle!!,
                            correlationIdKey to packet[correlationIdKey]
                        )
                    ).toJson())
            }
        }
}

private val Opplysningstyper.personRolle: PersonRolle?
    get() = when (this) {
        Opplysningstyper.AVDOED_SOEKNAD_V1, Opplysningstyper.AVDOED_PDL_V1 -> PersonRolle.AVDOED
        Opplysningstyper.SOEKER_SOEKNAD_V1, Opplysningstyper.SOEKER_PDL_V1 -> PersonRolle.BARN
        Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1 -> PersonRolle.GJENLEVENDE
        else -> null
    }

