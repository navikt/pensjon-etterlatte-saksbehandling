package no.nav.etterlatte.rivers.migrering

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.VedtaksbrevService
import no.nav.etterlatte.brev.hentinformasjon.VedtaksvurderingService
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
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
            validate { it.requireKey(UTLANDSTILKNYTNINGTYPE) }
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
        val utlandstilknytningType = packet[UTLANDSTILKNYTNINGTYPE].asText().let { UtlandstilknytningType.valueOf(it) }
        val migreringBrevRequest =
            MigreringBrevRequest(brutto = sum, yrkesskade = false, utlandstilknytningType = utlandstilknytningType)
        runBlocking {
            val sakId = vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo).sak.id
            val vedtaksbrev: Brev =
                service.opprettVedtaksbrev(
                    sakId,
                    behandlingId,
                    brukerTokenInfo,
                    migreringBrevRequest,
                )
            service.genererPdf(vedtaksbrev.id, brukerTokenInfo, migreringBrevRequest)
            service.ferdigstillVedtaksbrev(behandlingId, brukerTokenInfo, true)
            logger.info("Har oppretta vedtaksbrev for behandling $behandlingId")

            packet.eventName = VedtakKafkaHendelseType.ATTESTERT.toString()
            val vedtak = vedtaksvurderingService.hentVedtak(behandlingId, brukerTokenInfo)
            packet["vedtak"] = vedtak
        }
        context.publish(packet.toJson())
    }
}

const val SUM = "sum"
const val UTLANDSTILKNYTNINGTYPE = "UTLANDSTILKNYTNINGTYPE"

val behandlingerAaJournalfoereBrevFor =
    listOf<Triple<String, Int, UtlandstilknytningType>>(
        Triple("278da3a1-93f6-46d6-a8a9-7f0d602bbba6", 643, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("8c7e86a9-abf0-430f-ad10-dc6a44e501cf", 643, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("271b46dc-e38d-4ce6-9a5a-cb4ac0e63329", 3855, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("ab2615fc-3a84-4687-bc24-813852a56ed0", 2966, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("7e120cbf-f180-4cce-b515-3c34217666ef", 3954, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("1c96ae9d-5c2c-41d3-a762-267b0cf9acac", 3954, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("f2ca8e19-4c4f-4691-8521-f996a7f8c9ea", 3954, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("3e234ecc-f37b-4ceb-9a52-22d33c0c2187", 3262, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("2ef30da9-ac8a-4f41-ba4b-64116a36c444", 2198, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("5867fb63-8a4a-45c7-b27e-4105d4b325de", 3213, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("5d086725-67cc-4b54-8a58-6b504c29b821", 3954, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("8d219cd8-03ec-4442-a412-956fdeff594b", 3954, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("16f36950-1e9d-4b52-a21c-26d229caaf28", 3954, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("291655c4-1c25-4e0d-8641-21d873102115", 2060, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("2eb184c5-b8f9-4b35-a29b-67bf35e9125b", 2060, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("f194c296-13cb-46aa-80d3-a15049dcea11", 2060, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("86276cf2-f8b8-4d0f-b792-9b74bed79352", 2060, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("eed3e88e-5fad-4aef-9be9-6fbbc9bcf713", 3954, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("798b31e8-4f31-4e15-b4e4-04b702013088", 2471, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("73e794ec-3b54-4f75-9e70-d84529454c81", 321, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("aeead7bf-bdc5-4dc2-be53-8132c5c9cf51", 321, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("bbce5ee9-53ab-4456-af22-d1c3a21b0891", 2669, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("381e5f78-6353-4982-bc86-71224a1df914", 2669, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("4614a1c7-609c-48a9-9586-ef8cb4bd873a", 2669, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("581831b1-959e-4d12-aae8-b38f40238db4", 3954, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("b0761657-9d03-4e76-996d-ce6336106f8e", 1779, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("91035b09-7d51-4e04-a0fc-5b8979d4c78e", 1779, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("0dcf98d8-fa7f-42c9-8d51-1146e940d7f3", 1779, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("1c4c9337-173f-4c14-abaf-ad5b3d74a14f", 2811, UtlandstilknytningType.BOSATT_UTLAND),
        Triple("c49c887c-2a1f-4f6c-a89d-3f3bbc6791ae", 2811, UtlandstilknytningType.BOSATT_UTLAND),
    )
