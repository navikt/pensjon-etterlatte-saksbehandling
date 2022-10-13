package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

class GrunnlagEndretRiver(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(GrunnlagEndretRiver::class.java)

    init {
        // Kopi av river for eksisterende app
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GRUNNLAGENDRET")
            validate { it.requireKey(BehandlingGrunnlagEndretMedGrunnlag.grunnlagV2Key) }
            validate { it.requireKey("behandlingOpprettet") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("behandling.type") }
            validate { it.requireKey("sak.sakType") }
            validate { it.requireKey("fnrSoeker") }
            validate { it.interestedIn(BehandlingGrunnlagEndret.revurderingAarsakKey) }
            validate { it.interestedIn(BehandlingGrunnlagEndret.manueltOpphoerAarsakKey) }
            validate { it.interestedIn(BehandlingGrunnlagEndret.manueltOpphoerfritekstAarsakKey) }
            validate { it.rejectKey("vilkaarsvurdering") }
            validate { it.rejectKey("kommerSoekerTilGode") }
            validate { it.rejectKey("gyldighetsvurdering") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                val grunnlagEndretPayload = packet.toJson()
                val behandlingId = packet["behandlingId"].asText()

                val behandlingType = BehandlingType.valueOf(packet["behandling.type"].asText())
                val sakType = SakType.valueOf(packet["sak.sakType"].asText())

                // Må få kopiert over alle disse tingene med virkningsdato osv
                val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)

                // Inntil videre oppretter / oppdateres vilkårsvurdering med nyeste payload fra grunnlag
                if (vilkaarsvurdering != null) {
                    logger.info("Oppdaterer eksisterende vilkårsvurdering for behandlingId=$behandlingId")
                    vilkaarsvurderingService.oppdaterVilkaarsvurdering(behandlingId, grunnlagEndretPayload)
                } else {
                    logger.info("Oppretter ny vilkårsvurdering for behandlingId=$behandlingId")
                    vilkaarsvurderingService.opprettVilkaarsvurdering(
                        behandlingId,
                        sakType,
                        behandlingType,
                        grunnlagEndretPayload
                    )
                }
            } catch (e: Exception) {
                logger.error("En feil oppstod", e)
            }
        }
}

enum class SakType { BARNEPENSJON, OMSTILLINGSSTOENAD }