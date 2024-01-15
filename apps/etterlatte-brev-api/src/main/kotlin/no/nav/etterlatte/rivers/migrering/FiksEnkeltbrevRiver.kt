package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
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
    private val service: VedtaksbrevService,
    private val vedtaksvurderingService: VedtaksvurderingService,
) : ListenerMedLoggingOgFeilhaandtering(BrevEventTypes.FIKS_ENKELTBREV.toString()) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.FIKS_ENKELTBREV) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(SUM) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        logger.info("Fikser vedtaksbrev for behandling $behandlingId")

        val brukerTokenInfo = Systembruker("migrering", "migrering")
        val sum = packet[SUM].asInt()
        val migreringBrevRequest =
            MigreringBrevRequest(brutto = sum, yrkesskade = false, utlandstilknytningType = null)
        runBlocking {
            val sakId = retryOgPakkUt { vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo).sak.id }
            val vedtaksbrev: Brev =
                retryOgPakkUt {
                    service.opprettVedtaksbrev(
                        sakId,
                        behandlingId,
                        brukerTokenInfo,
                        migreringBrevRequest,
                    )
                }
            retryOgPakkUt { service.genererPdf(vedtaksbrev.id, brukerTokenInfo, migreringBrevRequest) }
            retryOgPakkUt { service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo, true) }
            logger.info("Har oppretta vedtaksbrev for behandling $behandlingId")

            packet.eventName = VedtakKafkaHendelseType.ATTESTERT.toString()
            val vedtak = retryOgPakkUt { vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo) }
            packet["vedtak"] = vedtak
        }
        context.publish(packet.toJson())
    }
}

const val SUM = "sum"

val behandlingerAaJournalfoereBrevFor =
    listOf<Triple<String, Int, UtlandstilknytningType?>>(
        Triple("4e1f7e4e-70bb-4794-b1a6-4cc899361995", 3954, null),
        Triple("7ac97ed5-2bf5-4142-a0a5-674b4f58b416", 2650, null),
        Triple("f2ca8e19-4c4f-4691-8521-f996a7f8c9ea", 3954, null),
        Triple("16f36950-1e9d-4b52-a21c-26d229caaf28", 3954, null),
    )
