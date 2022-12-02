import com.fasterxml.jackson.module.kotlin.readValue
import model.AvkortingService
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.event.BehandlingGrunnlagEndretMedGrunnlag
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class LesAvkortingsmelding(
    rapidsConnection: RapidsConnection,
    private val avkorting: AvkortingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LesAvkortingsmelding::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("BEHANDLING:GRUNNLAGENDRET")
            validate { it.requireKey(BehandlingGrunnlagEndretMedGrunnlag.grunnlagKey) }
            validate { it.requireKey("vilkaarsvurdering") }
            validate { it.requireKey("beregning") }
            validate { it.rejectKey("avkorting") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val beregningDTO = objectMapper.readValue(
                packet["beregning"].toString(),
                BeregningDTO::class.java
            )
            val beregningsResultat = BeregningsResultat(
                id = beregningDTO.beregningId,
                type = Beregningstyper.GP,
                endringskode = Endringskode.NY,
                resultat = BeregningsResultatType.BEREGNET,
                beregningsperioder = beregningDTO.beregningsperioder,
                beregnetDato = LocalDateTime.from(beregningDTO.beregnetDato.toNorskTid()),
                grunnlagVersjon = beregningDTO.grunnlagMetadata.versjon
            )
            try {
                val avkortingsResultat = avkorting.avkortingsResultat(beregningsResultat)

                packet["avkorting"] = avkortingsResultat
                context.publish(packet.toJson())
                logger.info("Publisert en beregning fra avkorting")
            } catch (e: Exception) {
                logger.error("spiser en melding på grunn av feil", e)
            }
        }
}