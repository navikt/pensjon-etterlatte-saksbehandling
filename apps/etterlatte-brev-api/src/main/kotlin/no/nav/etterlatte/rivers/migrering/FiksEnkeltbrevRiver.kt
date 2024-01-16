package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rivers.BrevEventTypes
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering

internal class FiksEnkeltbrevRiver(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtaksvurderingService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.FIKS_ENKELTBREV.toString()) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.FIKS_ENKELTBREV) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Fikser vedtaksbrev for behandling $behandlingId")

        val brukerTokenInfo = Systembruker("migrering", "migrering")
        runBlocking {
            packet.eventName = VedtakKafkaHendelseType.ATTESTERT.toString()
            val vedtak = retryOgPakkUt { vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo) }
            packet["vedtak"] = vedtak
        }
        context.publish(packet.toJson())
    }
}

val behandlingerAaJournalfoereBrevFor =
    listOf(
        "18118b9e-78ed-4d9a-b0de-b59c4a6bc061",
        "81049810-a71f-4d72-84e8-d8c2343683ed",
        "1d58ad45-d777-4fab-a92d-4e5e8f183850",
        "1f902cb0-13ae-415b-968a-957c75dc3c3b",
        "f54392e6-948b-4b31-b8f1-37fa249252f9",
    )
