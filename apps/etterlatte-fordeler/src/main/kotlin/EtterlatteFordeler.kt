package no.nav.etterlatte


import io.ktor.client.features.ResponseException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummer
import no.nav.etterlatte.libs.common.soeknad.SoeknadType
import no.nav.etterlatte.pdl.PersonService
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.OffsetDateTime

internal class EtterlatteFordeler(
    rapidsConnection: RapidsConnection,
    private val personService: PersonService,
    private val fordelerKriterierService: FordelerKriterierService,
    private val klokke: Clock = Clock.systemUTC()
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(EtterlatteFordeler::class.java)

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

        if (packet["@skjema_info"]["type"] == null || packet["@skjema_info"]["type"].asText() != SoeknadType.Barnepensjon.name.uppercase()) {
            logger.info("Avbrutt fordeling da søknad ikke er " + SoeknadType.Barnepensjon.name)
            return
        }

        runBlocking {
            try {
                val barnFnr = Foedselsnummer.of(packet["@fnr_soeker"].asText())
                val gjenlevendeFnr = Foedselsnummer.of(finnGjennlevendeFnr(packet))
                val avdoedFnr = Foedselsnummer.of(finnAvdoedFnr(packet))

                val barn = personService.hentUtvidetPerson(barnFnr, adresse = true, familieRelasjon = true, utland = true)
                val avdoed = personService.hentUtvidetPerson(avdoedFnr, utland = true, adresse = true)
                val gjenlevende = personService.hentUtvidetPerson(gjenlevendeFnr, adresse = true, familieRelasjon = true)

                val fordelerResultat = fordelerKriterierService.sjekkMotKriterier(
                    barn = barn,
                    avdoed = avdoed,
                    gjenlevende = gjenlevende,
                    packet = packet
                )

                if (fordelerResultat.kandidat) {
                    packet["@soeknad_fordelt"] = fordelerResultat.kandidat
                    packet["@event_name"] = "ey_fordelt"
                    logger.info("Fant en sak til Saksbehandling POC")
                    context.publish(packet.toJson())
                } else {
                    logger.info("Avbrutt fordeling, kriterier: " + fordelerResultat.forklaring.toString())
                    return@runBlocking
                }

            } catch (err: InvalidFoedselsnummer) {
                logger.error("Ugyldig fødselsnummer: ${err.message}", err)
            } catch (err: Exception) {
                logger.error("Uhaandtert feilsituasjon: ${err.message}", err)
            }
        }
    }

    private fun finnAvdoedFnr(sok: JsonMessage): String {
        return sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "AVDOED" }
            .map { it["foedselsnummer"].first()}[0].asText()
    }

    private fun finnGjennlevendeFnr(sok: JsonMessage): String {
        return sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "GJENLEVENDE_FORELDER" }
            .map { it["foedselsnummer"].first()}[0].asText()
    }

}
