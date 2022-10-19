package no.nav.etterlatte.vilkaarsvurdering

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.behandling.BehandlingType
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
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.toUUID
import org.slf4j.LoggerFactory

class GrunnlagEndretRiver(
    rapidsConnection: RapidsConnection,
    private val vilkaarsvurderingService: VilkaarsvurderingService
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(GrunnlagEndretRiver::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GRUNNLAGENDRET")
            validate { it.requireKey(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey) }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("behandling.id") }
            validate { it.requireKey("behandling.type") }
            validate { it.requireKey("behandling.soeknadMottattDato") }
            validate { it.requireKey("sak.sakType") }
            validate { it.rejectKey("vilkaarsvurdering") }
            validate { it.rejectKey("kommerSoekerTilGode") }
            validate { it.rejectKey("gyldighetsvurdering") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                val kafkaPayload = packet.toJson()
                val behandlingId = packet["behandling.id"].asText().toUUID()
                val behandlingType = packet["behandling.type"].asText().let { BehandlingType.valueOf(it) }
                val soeknadMottattDato = packet["behandling.soeknadMottattDato"].asLocalDateTime()
                val sakType = SakType.valueOf(packet["sak.sakType"].asText())
                val grunnlag = objectMapper.treeToValue<Grunnlag>(
                    packet[BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey]
                )

                if (nokGrunnlagErInnhentet(grunnlag)) {
                    val vilkaarsvurdering = vilkaarsvurderingService.hentVilkaarsvurdering(behandlingId)
                    if (vilkaarsvurdering != null) {
                        logger.info("Oppdaterer eksisterende vilkårsvurdering for behandlingId=$behandlingId")
                        vilkaarsvurderingService.oppdaterVilkaarsvurderingPayload(behandlingId, kafkaPayload)
                    } else {
                        // TODO denne skal flyttes ut men vi trenger den for å få sparket i gang disse greiene her
                        val virkningstidspunkt =
                            beregnVirkningstidspunktFoerstegangsbehandling(grunnlag, soeknadMottattDato.toLocalDate())

                        logger.info("Oppretter ny vilkårsvurdering for behandlingId=$behandlingId")
                        vilkaarsvurderingService.opprettVilkaarsvurdering(
                            behandlingId = behandlingId,
                            sakType = sakType,
                            behandlingType = behandlingType,
                            virkningstidspunkt = virkningstidspunkt,
                            grunnlag = grunnlag,
                            kafkaPayload = kafkaPayload
                        )
                    }
                }
            } catch (e: Exception) {
                // TODO Se på flyten her - denn skal muligens kastes hele veien ut
                logger.error("En feil oppstod", e)
            }
        }

    // TODO Midlertidig fiks for å unngå at behandling forsøkes å opprettes
    private fun nokGrunnlagErInnhentet(grunnlag: Grunnlag): Boolean {
        // Vil kaste NPE dersom grunnlag ikke eksisterer for avdød.
        grunnlag.hentAvdoed()
        return true
    }
}

// TODO Denne bør vel flyttes?
enum class SakType { BARNEPENSJON, OMSTILLINGSSTOENAD }