package no.nav.etterlatte.rivers

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.BrevParametereAutomatisk
import no.nav.etterlatte.brev.BrevParametereAutomatisk.OmstillingsstoenadInntektsjusteringRedigerbar
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.SaksbehandlerOgAttestant
import no.nav.etterlatte.brev.model.BrevDistribusjonResponse
import no.nav.etterlatte.brev.model.OpprettJournalfoerOgDistribuerRequest
import no.nav.etterlatte.klienter.BrevapiKlient
import no.nav.etterlatte.libs.common.rapidsandrivers.setEventNameForHendelseType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.brevId
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class InntektsjusteringVarselOgVedtakRiver(
    private val brevapiKlient: BrevapiKlient,
    rapidsConnection: RapidsConnection,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevRequestHendelseType.INNTEKTSJUSTERING_VARSEL_OG_VEDTAK) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREVMAL_RIVER_KEY, Brevkoder.OMS_INNTEKTSJUSTERING_VARSEL.toString()) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        runBlocking {
            logger.info("Oppretter inntektsjustering varsel og vedtak for sakId=${packet["sakId"]}")
            val brevErdistribuert = opprettOgDistribuerVarselOgVedtak(packet.sakId)

            packet.setEventNameForHendelseType(
                if (brevErdistribuert.erDistribuert) BrevHendelseType.DISTRIBUERT else EventNames.FEILA,
            )

            packet.brevId = brevErdistribuert.brevId
            context.publish(packet.toJson())
        }
    }

    private suspend fun opprettOgDistribuerVarselOgVedtak(sakId: SakId): BrevDistribusjonResponse {
        val brevKode = Brevkoder.OMS_INNTEKTSJUSTERING_VARSEL
        val brevdata: BrevParametereAutomatisk = OmstillingsstoenadInntektsjusteringRedigerbar()

        try {
            val req =
                OpprettJournalfoerOgDistribuerRequest(
                    brevKode = brevKode,
                    brevParametereAutomatisk = brevdata,
                    avsenderRequest = SaksbehandlerOgAttestant(Fagsaksystem.EY.navn, Fagsaksystem.EY.navn),
                    sakId = sakId,
                )
            return brevapiKlient.opprettJournalFoerOgDistribuer(sakId, req)
        } catch (e: Exception) {
            val feilMelding = "Fikk feil ved opprettelse av inntektsjustering varsel og vedtak for sakId=$sakId"
            logger.error(feilMelding, e)
            throw OpprettJournalfoerOgDistribuerRiverException(
                feilMelding,
                e,
            )
        }
    }

    override fun kontekst() = Kontekst.INNTEKTSJUSTERING
}
