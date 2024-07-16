package no.nav.etterlatte.rivers.opprettogjournalfoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.BREVMAL_RIVER_KEY
import no.nav.etterlatte.brev.BrevHendelseType
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_STEG
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILMELDING_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.feilendeSteg
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.rapidsandrivers.BOR_I_UTLAND_KEY
import no.nav.etterlatte.rapidsandrivers.BREV_ID_KEY
import no.nav.etterlatte.rapidsandrivers.BREV_KODE
import no.nav.etterlatte.rapidsandrivers.ER_OVER_18_AAR
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.rapidsandrivers.KONTEKST_KEY
import no.nav.etterlatte.rapidsandrivers.Kontekst
import no.nav.etterlatte.rapidsandrivers.ListenerMedLogging
import no.nav.etterlatte.rapidsandrivers.SAK_ID_KEY
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory

internal class FeilendeOpprettJournalfoerOgDistribuer(
    private val rapidsConnection: RapidsConnection,
    private val opprettJournalfoerOgDistribuerService: OpprettJournalfoerOgDistribuerService,
) : ListenerMedLogging() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, EventNames.FEILA) {
            validate { it.interestedIn(FEILENDE_STEG) }
            validate { it.requireAny(KONTEKST_KEY, listOf(Kontekst.BREV.name)) }
            validate { it.requireKey(FEILMELDING_KEY) }
            validate { it.requireKey(SAK_ID_KEY) }
            validate { it.requireKey(BREVMAL_RIVER_KEY) }
            validate { it.interestedIn(BOR_I_UTLAND_KEY) }
            validate { it.interestedIn(ER_OVER_18_AAR) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        if (packet.feilendeSteg == "OpprettJournalfoerOgDistribuerRiver") {
            logger.info("Håndterer OpprettJournalfoerOgDistribuerRiver med retry for sakid: ${packet.sakId}")
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
        } else {
            return
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
