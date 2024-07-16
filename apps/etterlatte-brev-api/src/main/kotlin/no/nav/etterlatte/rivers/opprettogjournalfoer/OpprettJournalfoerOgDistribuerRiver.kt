package no.nav.etterlatte.rivers.opprettogjournalfoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.BrevRequestHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.BOR_I_UTLAND_KEY
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BREV_KODE
import no.nav.etterlatte.rapidsandrivers.ER_OVER_18_AAR
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLoggingOgFeilhaandtering
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

class OpprettJournalfoerOgDistribuerRiver(
    private val rapidsConnection: RapidsConnection,
    private val opprettJournalfoerOgDistribuerService: OpprettJournalfoerOgDistribuerService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, BrevRequestHendelseType.OPPRETT_JOURNALFOER_OG_DISTRIBUER) {
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREVMAL_RIVER_KEY) }
            validate { it.interestedIn(BOR_I_UTLAND_KEY) }
            validate { it.interestedIn(ER_OVER_18_AAR) }
        }
    }

    override fun kontekst() = Kontekst.BREV

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        runBlocking {
            val brevkode = packet[BREVMAL_RIVER_KEY].asText().let { Brevkoder.valueOf(it) }
            val brevId =
                opprettJournalfoerOgDistribuerService.opprettJournalfoerOgDistribuer(
                    packet.sakId,
                    brevkode,
                    Systembruker.brev,
                    packet,
                )
            rapidsConnection.svarSuksess(packet.sakId, brevId, brevkode)
        }
    }

    private fun RapidsConnection.svarSuksess(
        sakId: Long,
        brevID: BrevID,
        brevkode: Brevkoder,
    ) {
        logger.info("Brev har blitt distribuert. Svarer tilbake med bekreftelse.")

        publish(
            sakId.toString(),
            JsonMessage
                .newMessage(
                    BrevHendelseType.DISTRIBUERT.lagEventnameForType(),
                    mapOf(
                        BREV_ID_KEY to brevID,
                        SAK_ID_KEY to sakId,
                        BREV_KODE to brevkode.name,
                    ),
                ).toJson(),
        )
    }
}
