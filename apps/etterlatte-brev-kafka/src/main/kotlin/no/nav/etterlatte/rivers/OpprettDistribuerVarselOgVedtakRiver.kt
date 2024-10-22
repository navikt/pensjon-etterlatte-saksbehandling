package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevParametereAutomatisk
import no.nav.etterlatte.brev.BrevParametereAutomatisk.OmstillingsstoenadInntektsjusteringRedigerbar
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.SaksbehandlerOgAttestant
import no.nav.etterlatte.brev.model.BrevDistribusjonResponse
import no.nav.etterlatte.brev.model.OpprettJournalfoerOgDistribuerRequest
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class OpprettDistribuerVarselOgVedtakRiver(
    private val brevapiKlient: BrevapiKlient,
    rapidsConnection: RapidsConnection,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevRequestHendelseType.OPPRETT_DISTRIBUER_VARSEL_OG_VEDTAK) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREVMAL_RIVER_KEY, Brevkoder.OMS_INNTEKTSJUSTERING_VARSEL.toString()) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        runBlocking {
            // TODO: opprett og distribuer brev
            // TODO: request to brev-api
            val brevkode = packet[BREVMAL_RIVER_KEY].asText().let { Brevkoder.valueOf(it) }
            val brevErdistribuert = opprettDistribuerVarselOgVedtak(packet.sakId, brevkode)

            /*packet.brevId = brevErdistribuert.brevId
            if (brevErdistribuert.erDistribuert) {
                packet.setEventNameForHendelseType(BrevHendelseType.DISTRIBUERT)
            } else {
                // Oppgave har blitt opprettet i brev-api hvis vi har kommet hit
                packet.setEventNameForHendelseType(EventNames.FEILA)
            }*/

            context.publish(packet.toJson())
        }
    }

    private suspend fun opprettDistribuerVarselOgVedtak(
        sakId: SakId,
        brevKode: Brevkoder,
    ): BrevDistribusjonResponse {
        logger.info("Oppretter $brevKode-brev i sak $sakId")
        val brevdata: BrevParametereAutomatisk =
            when (brevKode) {
                Brevkoder.OMS_INNTEKTSJUSTERING_VARSEL -> {
                    OmstillingsstoenadInntektsjusteringRedigerbar()
                }
                else -> throw Exception("Støtter ikke brevtype $brevKode i sak $sakId")
            }

        try {
            val req =
                OpprettJournalfoerOgDistribuerRequest(
                    brevKode = brevKode,
                    brevParametereAutomatisk = brevdata,
                    avsenderRequest = SaksbehandlerOgAttestant(Fagsaksystem.EY.navn, Fagsaksystem.EY.navn),
                    sakId = sakId,
                )

            // TODO: nytt endepunkt
            return brevapiKlient.opprettJournalFoerOgDistribuer(sakId, req)
        } catch (e: Exception) {
            val feilMelding = "Fikk feil ved opprettelse av brev for sak $sakId for brevkode: $brevKode"
            logger.error(feilMelding, e)
            throw OpprettJournalfoerOgDistribuerRiverException(
                feilMelding,
                e,
            )
        }
    }

    override fun kontekst() = Kontekst.BREV
}
