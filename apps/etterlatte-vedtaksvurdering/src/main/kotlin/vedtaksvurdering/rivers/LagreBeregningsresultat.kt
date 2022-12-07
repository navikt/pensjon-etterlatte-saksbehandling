package no.nav.etterlatte.vedtaksvurdering.rivers

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.KanIkkeEndreFattetVedtak
import no.nav.etterlatte.VedtaksvurderingService
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultatType
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import no.nav.etterlatte.libs.common.beregning.Endringskode
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTid
import no.nav.etterlatte.libs.common.vedtak.Behandling
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class LagreBeregningsresultat(
    rapidsConnection: RapidsConnection,
    val vedtaksvurderingService: VedtaksvurderingService
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(LagreBeregningsresultat::class.java)

    init {
        River(rapidsConnection).apply {
            validate { it.demandAny(eventNameKey, listOf("BEHANDLING:OPPRETTET", "BEHANDLING:GRUNNLAGENDRET")) }
            validate { it.requireKey("sakId") }
            validate { it.requireKey("behandling") }
            validate { it.requireKey("fnrSoeker") }
            validate { it.requireKey("beregning") }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            val behandling = objectMapper.readValue<Behandling>(packet["behandling"].toString())
            val beregningDTO = objectMapper.readValue(
                packet["beregning"].toString(),
                BeregningDTO::class.java
            )
            // TODO: SOS Endre til å bruke beregningDTO i databasen til vedtak hvis det skal ha det senere etter omskrivning
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
                vedtaksvurderingService.lagreBeregningsresultat(
                    behandling,
                    packet["fnrSoeker"].textValue(),
                    beregningsResultat
                )
                requireNotNull(vedtaksvurderingService.hentVedtak(behandling.id)).also {
                    context.publish(
                        JsonMessage.newMessage(
                            mapOf(
                                eventNameKey to "VEDTAK:BEREGNET",
                                "sakId" to it.sakId.toString(),
                                "behandlingId" to it.behandlingId.toString(),
                                "vedtakId" to it.id,
                                "eventtimestamp" to Tidspunkt.now()
                            )
                        ).toJson()
                    )
                }
            } catch (e: KanIkkeEndreFattetVedtak) {
                packet[eventNameKey] = "VEDTAK:ENDRING_FORKASTET"
                packet["vedtakId"] = e.vedtakId
                packet["forklaring"] = "Beregning forkastet fordi vedtak allerede er fattet"
                context.publish(
                    packet.toJson()
                )
            } catch (e: Exception) {
                logger.warn("Kunne ikke oppdatere vedtak", e)
            }
        }
}