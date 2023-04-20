package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.SoeknadType
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.rapidsandrivers.BEHOV_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class FordeltSoeknadRiver(
    rapidsConnection: RapidsConnection,
    private val gyldigSoeknadService: GyldigSoeknadService,
    private val behandlingClient: BehandlingClient
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(FordeltSoeknadRiver::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(SoeknadInnsendt.eventNameBehandlingBehov)
            correlationId()
            validate { it.requireKey(FordelerFordelt.skjemaInfoKey) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoTypeKey, SoeknadType.BARNEPENSJON.name) }
            validate { it.rejectKey(GyldigSoeknadVurdert.behandlingIdKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            logger.info("Starter gyldighetsvurdering av mottatt søknad om barnepensjon")

            try {
                val soeknad = packet.soeknad()
                val personGalleri = gyldigSoeknadService.hentPersongalleriFraSoeknad(soeknad)
                val sak = behandlingClient.skaffSak(personGalleri.soeker, SakType.BARNEPENSJON.toString())
                val gyldighetsVurdering = gyldigSoeknadService.vurderGyldighet(personGalleri, sak.sakType)
                logger.info("Gyldighetsvurdering utført: {}", gyldighetsVurdering)

                val behandlingId = behandlingClient.initierBehandling(sak.id, soeknad.mottattDato, personGalleri)
                behandlingClient.lagreGyldighetsVurdering(behandlingId, gyldighetsVurdering)
                logger.info("Behandling {} startet på sak {}", behandlingId, sak.id)

                sendOpplysningsbehov(sak, personGalleri, context, packet)

                context.publish(
                    packet.apply {
                        set(GyldigSoeknadVurdert.sakIdKey, sak.id)
                        set(GyldigSoeknadVurdert.behandlingIdKey, behandlingId)
                    }.toJson()
                )

                logger.info("Vurdert gyldighet av søknad er fullført")
            } catch (e: Exception) {
                logger.error("Gyldighetsvurdering av søknad om barnepensjon feilet", e)
            }
        }

    private fun sendOpplysningsbehov(
        sak: Sak,
        persongalleri: Persongalleri,
        context: MessageContext,
        packet: JsonMessage
    ) {
        context.publish(
            JsonMessage.newMessage(
                mapOf(
                    BEHOV_NAME_KEY to Opplysningstype.SOEKER_PDL_V1,
                    "sakId" to sak.id,
                    "sakType" to sak.sakType,
                    "fnr" to persongalleri.soeker,
                    "rolle" to PersonRolle.BARN,
                    CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                )
            ).toJson()
        )

        persongalleri.gjenlevende.forEach { fnr ->
            context.publish(
                JsonMessage.newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                        "sakId" to sak.id,
                        "sakType" to sak.sakType,
                        "fnr" to fnr,
                        "rolle" to PersonRolle.GJENLEVENDE,
                        CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                    )
                ).toJson()
            )
        }

        persongalleri.avdoed.forEach { fnr ->
            context.publish(
                JsonMessage.newMessage(
                    mapOf(
                        BEHOV_NAME_KEY to Opplysningstype.AVDOED_PDL_V1,
                        "sakId" to sak.id,
                        "sakType" to sak.sakType,
                        "fnr" to fnr,
                        "rolle" to PersonRolle.AVDOED,
                        CORRELATION_ID_KEY to packet[CORRELATION_ID_KEY]
                    )
                ).toJson()
            )
        }
    }

    private fun JsonMessage.soeknad() = this[FordelerFordelt.skjemaInfoKey].let {
        objectMapper.treeToValue<Barnepensjon>(
            it
        )
    }
}