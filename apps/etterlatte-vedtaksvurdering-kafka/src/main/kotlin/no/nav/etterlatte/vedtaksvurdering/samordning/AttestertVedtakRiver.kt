package no.nav.etterlatte.vedtaksvurdering.samordning

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.migrering.ListenerMedLogging

internal class AttestertVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtakService,
) : ListenerMedLogging() {
    init {
        initialiserRiver(rapidsConnection, "VEDTAK:ATTESTERT") {
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
        logger.info("Behandle til_samordning for vedtak [behandlingId=${vedtak.behandlingId}]")

        try {
            vedtaksvurderingService.tilSamordningVedtak(vedtak.behandlingId)
            logger.info("Behandlet til_samordning ferdig for vedtak [behandlingId=${vedtak.behandlingId}]")
        } catch (e: Exception) {
            logger.error("Feil ved oppdatering av vedtak til [TIL_SAMORDNET] for behandlingId: ${vedtak.behandlingId}", e)
        }
    }
}
