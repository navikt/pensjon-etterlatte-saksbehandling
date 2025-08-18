package no.nav.etterlatte.vedtaksvurdering.samordning

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import org.slf4j.LoggerFactory

internal class AttestertVedtakRiver(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtakService,
) : ListenerMedLogging() {
    init {
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.ATTESTERT) {
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
            throw e
        }
    }
}
