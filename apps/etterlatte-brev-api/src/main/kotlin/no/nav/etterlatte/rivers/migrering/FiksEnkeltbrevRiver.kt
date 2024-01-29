package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.token.Systembruker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import rapidsandrivers.behandlingId

internal class FiksEnkeltbrevRiver(
    rapidsConnection: RapidsConnection,
    private val vedtaksvurderingService: VedtaksvurderingService,
) : ListenerMedLoggingOgFeilhaandtering() {
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

        runBlocking {
            packet.setEventNameForHendelseType(VedtakKafkaHendelseHendelseType.ATTESTERT)
            val vedtak = retryOgPakkUt { vedtaksvurderingService.hentVedtak(behandlingId, Systembruker.migrering) }
            packet["vedtak"] = vedtak
        }
        context.publish(packet.toJson())
    }
}

val behandlingerAaJournalfoereBrevFor = listOf<String>()
