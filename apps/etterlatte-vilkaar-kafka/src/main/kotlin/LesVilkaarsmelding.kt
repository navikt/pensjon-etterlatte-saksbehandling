import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.barnepensjon.model.VilkaarService
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndret
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
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
            validate { it.requireKey("grunnlag") }
            validate { it.requireKey("behandlingOpprettet") }
            validate { it.requireKey("behandlingId") }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("fnrSoeker") }
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
                val grunnlag = requireNotNull(objectMapper.treeToValue<Grunnlag>(packet["grunnlag"]))
                val grunnlagForVilkaar = grunnlag.grunnlag.map {
                    VilkaarOpplysning(
                        it.id,
                        it.opplysningType,
                        it.kilde,
                        it.opplysning
                    )
                }
                val behandling = objectMapper.treeToValue<Behandling>(packet["behandling"])
                val behandlingopprettet = packet["behandlingOpprettet"].asLocalDateTime().toLocalDate()

                when (behandling.type) {
                    BehandlingType.FØRSTEGANGSBEHANDLING -> {
                        // TODO behandlingopprettet er feil dato å basere seg på, vi bør bruke søknad mottatt
                        val virkningstidspunkt = vilkaar.beregnVirkningstidspunktFoerstegangsbehandling(
                            grunnlagForVilkaar,
                            behandlingopprettet
                        )
                        val vilkaarsVurdering =
                            vilkaar.mapVilkaarForstegangsbehandling(grunnlagForVilkaar, virkningstidspunkt.atDay(1))
                        val kommerSoekerTilGodeVurdering = vilkaar.mapKommerSoekerTilGode(grunnlagForVilkaar)
                        packet["vilkaarsvurdering"] = vilkaarsVurdering
                        packet["virkningstidspunkt"] = virkningstidspunkt
                        packet["kommerSoekerTilGode"] = kommerSoekerTilGodeVurdering
                    }
                    BehandlingType.REVURDERING -> {
                        val revurderingAarsak: RevurderingAarsak = try {
                            RevurderingAarsak.valueOf(packet[BehandlingGrunnlagEndret.revurderingAarsakKey].asText())
                        } catch (e: Exception) {
                            logger.error(
                                "Fikk inn en revurderingsbehandling uten en gyldig behandlingsårsak: ${behandling.id}",
                                e
                            )
                            throw IllegalStateException(e)
                        }
                        val virkningstidspunkt =
                            vilkaar.beregnVirkningstidspunktRevurdering(grunnlagForVilkaar, revurderingAarsak)
                        val vilkaarsVurdering = vilkaar.mapVilkaarRevurdering(
                            grunnlagForVilkaar,
                            virkningstidspunkt.atDay(1),
                            revurderingAarsak
                        )
                        packet["vilkaarsvurdering"] = vilkaarsVurdering
                        packet["virkningstidspunkt"] = virkningstidspunkt
                    }
                    BehandlingType.MANUELT_OPPHOER -> {} // TODO  implementer
                }

                packet["vilkaarsvurderingGrunnlagRef"] = grunnlag.versjon
                context.publish(packet.toJson())

                logger.info(
                    "Vurdert vilkår for behandling med id=${behandling.id} og korrelasjonsid=${packet.correlationId}"
                )
            } catch (e: Exception) {
                logger.error(
                    "Vilkår kunne ikke vurderes på grunn av feil. Dette betyr at det ikke blir fylt ut en vilkårsvurdering for behandlingen for korrelasjonsid'en ${packet.correlationId}", // ktlint-disable max-line-length
                    e
                )
            }
        }
}