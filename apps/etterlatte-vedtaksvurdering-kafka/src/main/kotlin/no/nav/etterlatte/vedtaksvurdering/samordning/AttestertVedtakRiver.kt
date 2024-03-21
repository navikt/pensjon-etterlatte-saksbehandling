package no.nav.etterlatte.vedtaksvurdering.samordning

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.VedtakService
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
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

        val republiserteBehandlinger =
            listOf(
                "c8849fc0-924d-4350-bb56-1429732445c9",
                "16a0a8c2-6841-4745-9453-f915e8136d24",
                "b18e5209-ab30-458f-81d5-3c761059fac5",
                "5678d7f7-8782-4204-9a5a-38f25c06b1f8",
                "b4896813-dcd2-46ea-b3fd-e66c3e58dbb4",
            )
        if (vedtak.behandlingId.toString() in republiserteBehandlinger) {
            logger.info("Prøver ikke å sende til samordning for behandlinger manuelt fiksa, ${vedtak.behandlingId}")
            return
        }

        try {
            vedtaksvurderingService.tilSamordningVedtak(vedtak.behandlingId)
            logger.info("Behandlet til_samordning ferdig for vedtak [behandlingId=${vedtak.behandlingId}]")
        } catch (e: Exception) {
            logger.error("Feil ved oppdatering av vedtak til [TIL_SAMORDNET] for behandlingId: ${vedtak.behandlingId}", e)
            throw e
        }
    }
}
