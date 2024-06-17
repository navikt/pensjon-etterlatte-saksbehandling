package no.nav.etterlatte.utbetaling.iverksetting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingResponseDto
import no.nav.etterlatte.libs.common.utbetaling.UtbetalingStatusDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.migrering.FIKS_BREV_MIGRERING
import no.nav.etterlatte.sikkerLogg
import no.nav.etterlatte.utbetaling.avstemming.vedtak.Vedtaksverifiserer
import no.nav.etterlatte.utbetaling.common.UtbetalingEventDto
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.SendtTilOppdrag
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.IverksettResultat.UtbetalingslinjerForVedtakEksisterer
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetaling
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.UtbetalingService
import no.nav.etterlatte.utbetaling.iverksetting.utbetaling.Utbetalingsvedtak
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import java.util.UUID

data class KunneIkkeLeseVedtakException(
    val e: Exception,
) : RuntimeException(e)

class VedtakMottakRiver(
    rapidsConnection: RapidsConnection,
    private val utbetalingService: UtbetalingService,
    private val vedtaksverifiserer: Vedtaksverifiserer,
) : ListenerMedLogging() {
    init {
        // Barnepensjon
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.ATTESTERT) {
            validate { it.requireKey("vedtak") }
            validate { it.requireValue("vedtak.sak.sakType", SakType.BARNEPENSJON.name) }
            validate {
                it.requireAny(
                    "vedtak.type",
                    listOf(VedtakType.INNVILGELSE.name, VedtakType.OPPHOER.name, VedtakType.ENDRING.name),
                )
            }
            validate { it.rejectKey(FIKS_BREV_MIGRERING) }
        }

        // Omstillingsstønad
        initialiserRiver(rapidsConnection, VedtakKafkaHendelseHendelseType.SAMORDNET) {
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

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        var vedtakId: Long? = null
        try {
            val vedtak: Utbetalingsvedtak = lesVedtak(packet).also { vedtakId = it.vedtakId }
            // FIXME når VedtakNyDto tas i bruk: attestert ELLER samordnet (vedtakstatus)
            logger.info("Attestert vedtak med vedtakId=${vedtak.vedtakId} mottatt")

            vedtaksverifiserer.verifiser(vedtak)

            when (val resultat = utbetalingService.iverksettUtbetaling(vedtak)) {
                is SendtTilOppdrag -> {
                    logger.info("Vedtak med vedtakId=${vedtak.vedtakId} sendt til oppdrag - avventer kvittering")
                    sendUtbetalingSendtEvent(context, resultat.utbetaling)
                }

                is UtbetalingForVedtakEksisterer -> {
                    val feilmelding =
                        "Vedtak med vedtakId=${vedtak.vedtakId} eksisterer fra før. " +
                            "behandlingId for nytt vedtak: ${vedtak.behandling.id} - " +
                            "behandlingId for tidligere utbetaling: " +
                            "${resultat.eksisterendeUtbetaling.behandlingId.value}"
                    feilmelding.let {
                        logger.error(it)
                        sendUtbetalingFeiletEvent(
                            context,
                            vedtak.vedtakId,
                            vedtak.behandling.id,
                            it,
                        )
                    }
                }

                is UtbetalingslinjerForVedtakEksisterer -> {
                    (
                        "En eller flere utbetalingslinjer med id=[${resultat.utbetalingslinjeIDer()}] " +
                            "eksisterer fra før"
                    ).also {
                        logger.error(it)
                        sendUtbetalingFeiletEvent(
                            context,
                            vedtak.vedtakId,
                            vedtak.behandling.id,
                            it,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            val feilmelding = "En feil oppstod under prosessering av vedtak med vedtakId=$vedtakId"
            logger.error(feilmelding)
            sikkerLogg.error(feilmelding, e)
            sendUtbetalingFeiletEvent(context, vedtakId, null, feilmelding)

            if (feilSkalKastesVidere(e)) throw e
        }
    }

    private fun lesVedtak(packet: JsonMessage): Utbetalingsvedtak =
        try {
            val vedtak: VedtakDto = objectMapper.readValue(packet["vedtak"].toJson())
            Utbetalingsvedtak.fra(vedtak)
        } catch (e: Exception) {
            throw KunneIkkeLeseVedtakException(e)
        }

    private fun feilSkalKastesVidere(e: Exception): Boolean = e !is KunneIkkeLeseVedtakException

    private fun sendUtbetalingFeiletEvent(
        context: MessageContext,
        vedtakId: Long? = null,
        behandlingId: UUID?,
        beskrivelse: String,
    ) {
        context.publish(
            UtbetalingEventDto(
                utbetalingResponse =
                    UtbetalingResponseDto(
                        status = UtbetalingStatusDto.FEILET,
                        vedtakId = vedtakId,
                        behandlingId = behandlingId,
                        feilmelding = beskrivelse,
                    ),
            ).toJson(),
        )
    }

    private fun sendUtbetalingSendtEvent(
        context: MessageContext,
        utbetaling: Utbetaling,
    ) {
        context.publish(
            UtbetalingEventDto(
                utbetalingResponse =
                    UtbetalingResponseDto(
                        status = UtbetalingStatusDto.valueOf(utbetaling.status().name),
                        vedtakId = utbetaling.vedtakId.value,
                        behandlingId = utbetaling.behandlingId.value,
                    ),
            ).toJson(),
        )
    }

    private fun UtbetalingslinjerForVedtakEksisterer.utbetalingslinjeIDer() =
        this.utbetalingslinjer.joinToString(",") { it.id.value.toString() }

    companion object {
        private val logger = LoggerFactory.getLogger(VedtakMottakRiver::class.java)
    }
}
