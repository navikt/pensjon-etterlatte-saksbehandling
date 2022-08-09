package no.nav.etterlatte

import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class PdlHendelser(
    rapidsConnection: RapidsConnection,
    private val behandlinger: Behandling,

    ) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(OppdaterBehandling::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("PDL:PERSONHENDELSE")
            correlationId()
            validate { it.requireKey("hendelse") }
            validate { it.interestedIn("avdoed_fnr", "avdoed_doedsdato") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            logger.info("Mottatt hendelse fra pdl: ${packet["hendelse"]}")
            val avdoedFnr = packet["avdoed_fnr"].asText()

            val avdoedDoedsdato: LocalDate? = try {
                packet["avdoed_doedsdato"].asLocalDate()
            } catch (e: Exception) {
                logger.warn(
                    "Kunne ikke parse dødsdato for hendelse med correlation id='${packet.correlationId}': " +
                            "$packet på grunn av feil. Verdien for avdoed_doedsdato er: ${packet["avdoed_doedsdato"].asText()} Vi bruker null som dødsdato for denne hendelsen, men dette er " +
                            "sannsynligvis en bug.", e
                )
                null
            }
            val doedshendelse = Doedshendelse(avdoedFnr, avdoedDoedsdato)
            behandlinger.sendDoedshendelse(doedshendelse)
        }
}
