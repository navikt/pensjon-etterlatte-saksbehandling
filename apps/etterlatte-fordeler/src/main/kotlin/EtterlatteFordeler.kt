package no.nav.etterlatte

import io.ktor.client.features.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.toUpperCasePreservingASCIIRules
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelseResponse
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.pdl.Person
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
    private lateinit var barn: Person
    val kriterier = listOf(
        Kriterie("Bosatt Utland") { bosattUtland() },
        Kriterie("Barn er for gammelt") { barnForGammel() },
        Kriterie("Barn har adressebeskyttelse") { harAdressebeskyttelse() }
    )
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "soeknad_innsendt") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("@template") }
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
        //TODO denne må skrives om til å håndtere manglende soeknads_type
        //TODO løfte opp konstant
        if(packet["@skjema_info"]["type"].asText() != "BARNEPENSJON")
        {
            logger.info("Avbrutt fordeling da søknad ikke er barnepensjon")
            return
        }
        val barnFnr = Foedselsnummer.of(packet["@fnr_soeker"].asText())
        runBlocking {
             barn = personService.hentPerson(barnFnr)
        }
        try {
            val aktuelleSaker = fordel(packet)
            if(aktuelleSaker.kandidat) {
                packet["@soeknad_fordelt"] = aktuelleSaker.kandidat
                packet["@event_name"] = "ey_fordelt"
                logger.info("Fant en sak til Saksbehandling POC")
                context.publish(packet.toJson())
            }
            else
            {
                logger.info("Avbrutt fordeling, kriterier: " + aktuelleSaker.forklaring.toString())
                return
            }
        } catch (err: ResponseException) {
            logger.error("duplikat: ", err)
            logger.error(packet["@soeknad_fordelt"].asText())
        } catch (err: Exception) {
            logger.error("Uhaandtert feilsituasjon: ", err)
        }
    }

    private fun harAdressebeskyttelse(): Boolean {
        return barn.adressebeskyttelse
    }

    private fun barnForGammel(): Boolean {
        //TODO endre logikk
        return barn.foedselsaar!! < 2006
    }

    private fun bosattUtland(): Boolean {
        //TODO
        // bytte ut sjekk av statsborgerskap med sjekk av utlandsopphold

        return barn.statsborgerskap != "uvisst" && barn.statsborgerskap != "NOR"

    }
    data class FordelRespons (
        val kandidat: Boolean,
        val forklaring: List<String>
    )

    class Kriterie(val forklaring: String, private val sjekk: Sjekk) {
        fun blirOppfyltAv(message: JsonMessage):Boolean = sjekk(message)
    }

    private fun fordel(packet: JsonMessage): FordelRespons{
        return kriterier
            .filter{it.blirOppfyltAv(packet)}
            .map { it.forklaring }
            .let { FordelRespons(it.isEmpty(), it) }
    }
}
typealias Sjekk = (JsonMessage)->Boolean