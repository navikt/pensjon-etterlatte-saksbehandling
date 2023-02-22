package no.nav.etterlatte.gyldigsoeknad.omstillingsstoenad

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.gyldigsoeknad.client.BehandlingClient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.GyldigSoeknadVurdert
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat.OPPFYLT
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.omstillingsstoenad.Omstillingsstoenad
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class InnsendtSoeknadRiver(
    rapidsConnection: RapidsConnection,
    private val gyldigOmstillingsSoeknadService: GyldigOmstillingsSoeknadService,
    private val behandlingClient: BehandlingClient
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(InnsendtSoeknadRiver::class.java)

    init {
        River(rapidsConnection).apply {
            eventName(SoeknadInnsendt.eventName)
            correlationId()
            validate { it.requireKey(SoeknadInnsendt.skjemaInfoKey) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoTypeKey, SoeknadType.OMSTILLINGSSTOENAD.name) }
            validate { it.demandValue(SoeknadInnsendt.skjemaInfoVersjonKey, "1") }
            validate { it.requireKey(SoeknadInnsendt.lagretSoeknadIdKey) }
            validate { it.requireKey(SoeknadInnsendt.hendelseGyldigTilKey) }
            validate { it.requireKey(SoeknadInnsendt.adressebeskyttelseKey) }
            validate { it.requireKey(SoeknadInnsendt.fnrSoekerKey) }
            validate { it.rejectKey(SoeknadInnsendt.dokarkivReturKey) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLogContext(packet.correlationId) {
            try {
                val soeknad = packet.soeknad()
                val personGalleri = gyldigOmstillingsSoeknadService.hentPersongalleriFraSoeknad(soeknad)
                val gyldighetsVurdering = gyldigOmstillingsSoeknadService.vurderGyldighet(personGalleri)
                logger.info("Gyldighetsvurdering utført: {}", gyldighetsVurdering)

                val sakId = behandlingClient.skaffSak(personGalleri.soeker, SakType.OMSTILLINGSSTOENAD.name)
                val behandlingId = behandlingClient.initierBehandling(sakId, soeknad.mottattDato, personGalleri)
                behandlingClient.lagreGyldighetsVurdering(behandlingId, gyldighetsVurdering)
                logger.info("Behandling {} startet på sak {}", behandlingId, sakId)

                context.publish(
                    packet.apply {
                        set(eventNameKey, GyldigSoeknadVurdert.eventName)
                        set(GyldigSoeknadVurdert.sakIdKey, sakId)
                        set(GyldigSoeknadVurdert.behandlingIdKey, behandlingId)
                        set(GyldigSoeknadVurdert.gyldigInnsenderKey, gyldighetsVurdering.resultat == OPPFYLT)
                    }.toJson()
                )
                logger.info("Vurdert gyldighet av søknad om omstillingsstønad er fullført")
            } catch (e: Exception) {
                logger.error("Gyldighetsvurdering av søknad om omstillingsstønad feilet", e)
            }
        }
    }

    private fun JsonMessage.soeknad() = this[FordelerFordelt.skjemaInfoKey].let {
        objectMapper.treeToValue<Omstillingsstoenad>(
            it
        )
    }
}