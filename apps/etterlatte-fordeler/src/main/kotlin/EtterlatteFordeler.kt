package no.nav.etterlatte.prosess

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.ktor.client.features.ResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.journalpost.JournalpostInfo
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.soeknad.SoeknadType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.OffsetDateTime

internal class EtterlatteFordeler(
    rapidsConnection: RapidsConnection,
    private val klokke: Clock = Clock.systemUTC()
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(EtterlatteFordeler::class.java)
    val kriterier = listOf(
        Kriterie("Ikke vært i norge hele livet") { bosattNorgeHeleLivet() },
        Kriterie("Barn er for gammelt") { barnForGammel() }
    )
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "soeknad_innsendt") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("@template") }
            validate { it.requireKey("@journalpostInfo") }
            validate { it.requireKey("@lagret_soeknad_id") }
            validate { it.requireKey("@hendelse_gyldig_til") }
            validate { it.requireKey("@adressebeskyttelse") }
            validate { it.requireKey("@fnr_soeker") }
            validate { it.rejectKey("@soeknad_fordelt") }
            validate { it.rejectKey("@dokarkivRetur") }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val gyldigTilDato = OffsetDateTime.parse(packet["@hendelse_gyldig_til"].asText())

        if (gyldigTilDato.isBefore(OffsetDateTime.now(klokke))) {
            logger.error("Avbrutt fordeling da hendelsen ikke er gyldig lengre")
            return
        }

        try {
            val fordelResponse = fordel(packet)
            packet["@soeknad_fordelt"] = fordelResponse
            packet["@event_name"] = "ey_fordelt"
            context.publish(packet.toJson())
        } catch (err: ResponseException) {
            logger.error("duplikat: ", err)
            logger.error(packet["@soeknad_fordelt"].asText())
        } catch (err: Exception) {
            logger.error("Uhaandtert feilsituasjon: ", err)
        }
    }

    private fun sjekkAdressebeskyttelse(): Boolean {
        //TODO
        return true
    }

    private fun hentPersonTest(): Boolean {
        //TODO
        return true
    }

    private fun barnForGammel(): Boolean {
        //TODO
        return true
    }

    private fun bosattNorgeHeleLivet(): Boolean {
        //TODO
        return true

    }
    data class fordelrespons (
        val kandidat: Boolean,
        val forklaring: List<String>
    )

    class Kriterie(val forklaring: String, private val sjekk: Sjekk) {
        fun blirOppfyltAv(message: JsonMessage):Boolean = sjekk(message)
    }

    private fun fordel(packet: JsonMessage){
        return kriterier
            .filter{it.blirOppfyltAv(packet)}
            .map { it.forklaring }
            .let { fordelrespons(it.isEmpty(), it) }
    }
}
typealias Sjekk = (JsonMessage)->Boolean