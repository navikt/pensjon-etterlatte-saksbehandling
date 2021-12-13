package no.nav.etterlatte

import io.ktor.client.features.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelseResponse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.pdl.PersonService
import no.nav.etterlatte.prosess.pdl.PersonResponse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.OffsetDateTime

internal class EtterlatteFordeler(
    rapidsConnection: RapidsConnection,
    private val personService : PersonService,
    private val klokke: Clock = Clock.systemUTC()
) : River.PacketListener {
    private val logger = LoggerFactory.getLogger(EtterlatteFordeler::class.java)
    private lateinit var barn: PersonResponse
    val kriterier = listOf(
        Kriterie("Ikke vært i norge hele livet") { bosattNorgeHeleLivet() },
        Kriterie("Barn er for gammelt") { barnForGammel() }
    )
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "soeknad_innsendt") }
            //TODO valideringen under funkerer ikke og må fikses.
            // validate { it.demandValue("@skjema_info"."@soeknad_type", "barnepensjon") }
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
        //TODO
        // Hente ut person her, med @fnr_soeker
        // Lagres i et personResponse-objekt
        runBlocking {
             barn = personService.hentPerson( Foedselsnummer.of(packet["@fnr_soeker"].asText()))
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
        return barn.alder() > 15
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