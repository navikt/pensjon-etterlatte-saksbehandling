
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.barnepensjon.model.VilkaarService
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
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

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
            validate { it.requireKey("fnrSoeker") }
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
                    VilkaarOpplysning(it.id,
                        it.opplysningType,
                        it.kilde,
                        it.opplysning)
                }
                val vilkaarsVurdering = vilkaar.mapVilkaar(grunnlagForVilkaar)
                val kommerSoekerTilGodeVurdering = vilkaar.mapKommerSoekerTilGode(grunnlagForVilkaar)
                val behandlingopprettet = LocalDateTime.parse(packet["behandlingOpprettet"].asText()).toLocalDate()

                packet["virkningstidspunkt"] = vilkaar.beregnVilkaarstidspunkt(grunnlagForVilkaar, behandlingopprettet)?: objectMapper.nullNode()
                packet["vilkaarsvurdering"] = vilkaarsVurdering
                packet["vilkaarsvurderingGrunnlagRef"] = grunnlag.versjon
                packet["kommerSoekerTilGode"] = kommerSoekerTilGodeVurdering
                context.publish(packet.toJson())

                logger.info("Vurdert Vilkår")
            } catch (e: Exception) {
                println("Vilkår kunne ikke vurderes: $e")
            }
        }
}


