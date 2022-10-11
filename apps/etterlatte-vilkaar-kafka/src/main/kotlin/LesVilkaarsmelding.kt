import barnepensjon.domain.Aarsak
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.barnepensjon.model.VilkaarService
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsgrunnlag
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class LesVilkaarsmelding(
    rapidsConnection: RapidsConnection,
    private val vilkaar: VilkaarService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesVilkaarsmelding::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GRUNNLAGENDRET")
            validate { it.requireKey("grunnlagV2") }
            validate { it.requireKey("behandlingOpprettet") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("behandling") }
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
                val grunnlag = requireNotNull(objectMapper.treeToValue<Opplysningsgrunnlag>(packet["grunnlagV2"]))
                val behandling = objectMapper.treeToValue<Behandling>(packet["behandling"])
                val behandlingopprettet = packet["behandlingOpprettet"].asLocalDateTime().toLocalDate()
                val revurderingAarsak: RevurderingAarsak? = kotlin.runCatching {
                    RevurderingAarsak.valueOf(packet[BehandlingGrunnlagEndret.revurderingAarsakKey].asText())
                }.getOrNull()
                val (fritekst, liste) = kotlin.runCatching {
                    val fritekstAarsakManueltOpphoer =
                        packet[BehandlingGrunnlagEndret.manueltOpphoerfritekstAarsakKey].asText()
                    val kjenteAarsakerManueltOpphoer: List<ManueltOpphoerAarsak> =
                        objectMapper.treeToValue(packet[BehandlingGrunnlagEndret.manueltOpphoerAarsakKey])
                    fritekstAarsakManueltOpphoer to kjenteAarsakerManueltOpphoer
                }.getOrNull() ?: (null to emptyList())
                val aarsak = Aarsak(
                    manueltOpphoerFritekstgrunn = fritekst,
                    manueltOpphoerKjenteGrunner = liste,
                    revurderingAarsak = revurderingAarsak
                )
                val (virkningstidspunkt, vilkaarsvurdering, kommerSoekerTilGode) =
                    vilkaar.finnVirkningstidspunktOgVilkaarForBehandling(
                        behandling,
                        grunnlag,
                        behandlingopprettet,
                        aarsak
                    )
                packet["vilkaarsvurdering"] = vilkaarsvurdering
                packet["virkningstidspunkt"] = virkningstidspunkt
                kommerSoekerTilGode?.let { packet["kommerSoekerTilGode"] = it }

                packet["vilkaarsvurderingGrunnlagRef"] = grunnlag.hentVersjon()
                context.publish(packet.toJson())

                logger.info(
                    "Vurdert vilkår for behandling med id=${behandling.id} og korrelasjonsid=${packet.correlationId}"
                )
            } catch (e: Exception) {
                logger.error(
                    """Vilkår kunne ikke vurderes på grunn av feil. Dette betyr at det ikke blir fylt ut en 
                        |vilkårsvurdering for behandlingen for korrelasjonsid'en 
                        |${packet.correlationId}
                    """.trimMargin(),
                    e
                )
            }
        }
}