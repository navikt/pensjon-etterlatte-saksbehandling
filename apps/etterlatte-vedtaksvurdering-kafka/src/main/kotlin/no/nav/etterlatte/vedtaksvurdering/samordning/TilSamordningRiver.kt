package no.nav.etterlatte.vedtaksvurdering.samordning

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.LoggerFactory

internal class TilSamordningRiver(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtakService,
) : ListenerMedLogging() {
    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.TIL_SAMORDNING) {
            validate { it.requireKey("vedtak") }
            validate { it.requireValue("vedtak.sak.sakType", SakType.OMSTILLINGSSTOENAD.name) }
            validate {
                it.requireAny(
                    "vedtak.type",
                    listOf(VedtakType.INNVILGELSE.name, VedtakType.OPPHOER.name, VedtakType.ENDRING.name),
                )
            }
        }
    }

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val vedtak = objectMapper.readValue<VedtakDto>(packet["vedtak"].toJson())
        val marker = createLogMarker(vedtak)
        logger.info(marker, "Behandler ekstern samordning [behandling=${vedtak.behandlingId}]")

        try {
            if (vedtaksvurderingService.samordneVedtak(vedtak.behandlingId).skalVentePaaSamordning) {
                logger.info(marker, "Skal vente på samordning [behandling=${vedtak.behandlingId}]")
            } else {
                // Dersom man ikke skal vente, så skal vedtaksstatus oppdateres til SAMORDNET
                logger.info(marker, "Ferdigstiller samordning [behandling=${vedtak.behandlingId}]")
                vedtaksvurderingService.samordnetVedtak(vedtak.id.toString())
                logger.info(marker, "Samordning ferdig [behandling=${vedtak.behandlingId}]")
            }
        } catch (e: Exception) {
            logger.error(marker, "Feil ved behandling av samordning [behandling=${vedtak.behandlingId}]")
            throw e
        }
    }

    private fun createLogMarker(vedtak: VedtakDto): LogstashMarker =
        Markers.appendEntries(
            mapOf<String, Any?>(
                "vedtakId" to vedtak.id,
                "behandlingId" to vedtak.behandlingId,
                "sakId" to vedtak.sak.id,
            ),
        )
}
