package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat.OPPFYLT
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
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
            eventName("soeknad_innsendt")
            correlationId()
            validate { it.demandValue(FordelerFordelt.soeknadFordeltKey, true) }
            validate { it.requireKey(FordelerFordelt.skjemaInfoKey) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoTypeKey, SoeknadType.BARNEPENSJON.name) }
            validate { it.rejectKey(GyldigSoeknadVurdert.sakIdKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            logger.info("Starter gyldighetsvurdering av mottatt søknad om barnepensjon")

            try {
                val soeknad = packet.soeknad()
                val personGalleri = gyldigSoeknadService.hentPersongalleriFraSoeknad(soeknad)
                val gyldighetsVurdering = gyldigSoeknadService.vurderGyldighet(personGalleri)
                logger.info("Gyldighetsvurdering utført: {}", gyldighetsVurdering)

                val sakId = behandlingClient.skaffSak(personGalleri.soeker, "BARNEPENSJON")
                val behandlingId = behandlingClient.initierBehandling(sakId, soeknad.mottattDato, personGalleri)
                behandlingClient.lagreGyldighetsVurdering(behandlingId, gyldighetsVurdering)
                logger.info("Behandling {} startet på sak {}", behandlingId, sakId)

                context.publish(
                    packet.apply {
                        set(GyldigSoeknadVurdert.sakIdKey, sakId)
                        set(GyldigSoeknadVurdert.behandlingIdKey, behandlingId)
                        set(GyldigSoeknadVurdert.gyldigInnsenderKey, gyldighetsVurdering.resultat == OPPFYLT)
                    }.toJson()
                )

                logger.info("Vurdert gyldighet av søknad er fullført")
            } catch (e: Exception) {
                logger.error("Gyldighetsvurdering av søknad om barnepensjon feilet", e)
            }
        }

    private fun JsonMessage.soeknad() = this[FordelerFordelt.skjemaInfoKey].let {
        objectMapper.treeToValue<Barnepensjon>(
            it
        )
    }
}