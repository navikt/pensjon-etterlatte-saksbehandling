package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.toUUID
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
            validate { it.requireKey(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey) }
            validate { it.requireKey("behandlingOpprettet") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("behandling.type") }
            validate { it.requireKey("sak.sakType") }
            validate { it.requireKey("fnrSoeker") }
            validate { it.interestedIn(BehandlingGrunnlagEndret.revurderingAarsakKey) }
            validate { it.interestedIn(BehandlingGrunnlagEndret.manueltOpphoerAarsakKey) }
            validate { it.interestedIn(BehandlingGrunnlagEndret.manueltOpphoerfritekstAarsakKey) }
            validate { it.interestedIn(BehandlingGrunnlagEndret.revurderingAarsakKey) }
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
                val behandlingId = packet["behandlingId"].asText().toUUID()
                val behandlingType = BehandlingType.valueOf(packet["behandling.type"].asText())
                val sakType = SakType.valueOf(packet["sak.sakType"].asText())
                val grunnlag =
                    requireNotNull(
                        objectMapper.treeToValue<Grunnlag>(
                            packet[BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey]
                        )
                    )
                val revurderingAarsak: RevurderingAarsak? = kotlin.runCatching {
                    RevurderingAarsak.valueOf(packet[BehandlingGrunnlagEndret.revurderingAarsakKey].asText())
                }.getOrNull()

                // todo: Midlertidig fiks for å unngå at behandling forsøkes å opprettes
                // Vil kaste NPE dersom grunnlag ikke eksisterer for avdød.
                grunnlag.hentAvdoed()

                // Må få kopiert over alle disse tingene med virkningsdato osv
                val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)

                // Inntil videre oppretter / oppdateres vilkårsvurdering med nyeste payload fra grunnlag for å
                // kunne sende dette videre sendere.
                if (vilkaarsvurdering != null) {
                    logger.info("Oppdaterer eksisterende vilkårsvurdering for behandlingId=$behandlingId")
                    vilkaarsvurderingService.oppdaterVilkaarsvurderingPayload(behandlingId, grunnlagEndretPayload)
                } else {
                    logger.info("Oppretter ny vilkårsvurdering for behandlingId=$behandlingId")
                    vilkaarsvurderingService.opprettVilkaarsvurdering(
                        behandlingId,
                        sakType,
                        behandlingType,
                        grunnlagEndretPayload,
                        grunnlag,
                        revurderingAarsak
                    )
                }
            } catch (e: Exception) {
                // TODO Se på flyten her - denn skal muligens kastes hele veien ut
                logger.error("En feil oppstod", e)
            }
        }
}

// TODO Denne bør vel flyttes?
enum class SakType { BARNEPENSJON, OMSTILLINGSSTOENAD }