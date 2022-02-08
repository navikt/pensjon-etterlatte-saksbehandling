package no.nav.etterlatte


import io.ktor.client.features.ResponseException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.InvalidFoedselsnummer
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.alder
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
    private val klokke: Clock = Clock.systemUTC()
) : River.PacketListener {

    //TODO flytte disse ned til onPacket for å sikre trådhåntering
    private val logger = LoggerFactory.getLogger(EtterlatteFordeler::class.java)
    private lateinit var barn: Person
    private lateinit var avdoed: Person
    private lateinit var gjenLevende: Person
    private lateinit var soeknad: JsonMessage

    /*
    *Dødsfallet er registrert
    *Alder - barn under 15 år
    Enebarn
    -Verge og foreldreansvar - ikke sjekket foreldreansvar. Kun sjekket verge fra søknad og at søker er forelder i søknaden
    *Ingen barn på vei
    *Yrkesskade
    *Avdød- ingen ut- og innvandringsdatoer
    *Avdød - Ikke oppgitt utenlandsopphold
    *Gjenlevende ektefelle/samboer - bosatt i Norge
    *Barnet - bosatt i Norge + ingen ut- og innvandringsdatoe
     */


    val kriterier = listOf(
        Kriterie("Barn er ikke norsk statsborger") { sjekkStatsborgerskap(barn) },
        Kriterie("Barn er for gammelt") { forGammel(barn, 15) },
        Kriterie("Barn har adressebeskyttelse") { harAdressebeskyttelse(barn) },
        Kriterie("Barn ikke fodt i Norge") { foedtUtland(barn) },
        Kriterie("Barn har utvandring") { harUtvandring(barn) },
        Kriterie("Avdoed har utvandring") { harUtvandring(avdoed) },
        Kriterie("Avdød har yrkesskade") { harYrkesskade(soeknad) },
        Kriterie("Søker er ikke forelder") { soekerIkkeForelder(soeknad) },
        Kriterie("Avdød er ikke død") { personErIkkeDoed(avdoed) },
        Kriterie("Barn har verge") { harVerge(soeknad) },
        Kriterie("Det er huket av for utenlandsopphold for avdøde") { harHuketAvForUtenlandsopphold(soeknad) },
        Kriterie("Barn er ikke bosatt i Norge") { ikkeGyldigBostedsAdresseINorge(barn) },
        Kriterie("Avdød er ikke bosatt i Norge") { ikkeGyldigBostedsAdresseINorge(avdoed) },
        Kriterie("Gjenlevende er ikke bosatt i Norge") { ikkeGyldigBostedsAdresseINorge(gjenLevende) },
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
        soeknad = packet

        if (gyldigTilDato.isBefore(OffsetDateTime.now(klokke))) {
            logger.error("Avbrutt fordeling da hendelsen ikke er gyldig lengre")
            return
        }
        //TODO må denne skrives om til å håndtere manglende soeknads_type?
        if (packet["@skjema_info"]["type"].asText() != SoeknadType.Barnepensjon.name.uppercase()) {
            logger.info("Avbrutt fordeling da søknad ikke er " + SoeknadType.Barnepensjon.name)
            return
        }

        runBlocking {

            try {
                val barnFnr = Foedselsnummer.of(packet["@fnr_soeker"].asText())
                val gjenlevendeFnr = Foedselsnummer.of(finnGjennlevendeFnr(packet))
                val avdoedFnr = Foedselsnummer.of(finnAvdoedFnr(packet))
                barn = personService.hentPerson(barnFnr)
                avdoed = personService.hentPerson(avdoedFnr)
                gjenLevende = personService.hentPerson(gjenlevendeFnr)
                barn.utland = personService.hentUtland(barnFnr)
                avdoed.utland = personService.hentUtland(avdoedFnr)
                barn.adresse = personService.hentAdresse(barnFnr, false)
                avdoed.adresse = personService.hentAdresse(avdoedFnr, false)
                gjenLevende.adresse = personService.hentAdresse(gjenlevendeFnr, false)

                val aktuelleSaker = fordel(packet)
                if (aktuelleSaker.kandidat) {
                    packet["@soeknad_fordelt"] = aktuelleSaker.kandidat
                    packet["@event_name"] = "ey_fordelt"
                    logger.info("Fant en sak til Saksbehandling POC")
                    context.publish(packet.toJson())
                } else {
                    logger.info("Avbrutt fordeling, kriterier: " + aktuelleSaker.forklaring.toString())
                    return@runBlocking
                }
            } catch (err: ResponseException) {
                logger.error("duplikat: ", err)
                logger.error(packet["@soeknad_fordelt"].asText())
            } catch (err: InvalidFoedselsnummer) {

                packet["@event_name"] = "ugyldigFnr"
                logger.info(err.message)
                context.publish(packet.toJson())

            } catch (err: Exception) {
                logger.error("Uhaandtert feilsituasjon: ", err)
            }
        }
    }

    private fun harAdressebeskyttelse(person: Person): Boolean {
        return person.adressebeskyttelse
    }

    private fun forGammel(person: Person, alder: Int): Boolean {
        return person.alder() > alder
    }

    private fun sjekkStatsborgerskap(person: Person): Boolean {
        return person.statsborgerskap != "NOR"
    }

    private fun foedtUtland(person: Person): Boolean {
        return person.foedeland != "NOR"
    }

    private fun harUtvandring(person: Person): Boolean {
        return (person.utland?.innflyttingTilNorge?.isNotEmpty() == true || person.utland?.utflyttingFraNorge?.isNotEmpty() == true)
    }

    private fun harYrkesskade(sok: JsonMessage): Boolean {
        return sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "AVDOED" }
            .filter { it["doedsaarsakSkyldesYrkesskadeEllerYrkessykdom"]["svar"].asText() == "JA" }
            .isNotEmpty()
    }

    //TODO bør vel endres til PDL familierelasjon
    private fun finnAvdoedFnr(sok: JsonMessage): String {
        return sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "AVDOED" }
            .map { it["foedselsnummer"] }
            .first().asText()
    }

    private fun finnGjennlevendeFnr(sok: JsonMessage): String {
        return sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "GJENLEVENDE_FORELDER" }
            .map { it["foedselsnummer"] }
            .first().asText()
    }

    private fun soekerIkkeForelder(sok: JsonMessage): Boolean {
        return sok["@skjema_info"]["innsender"]["foedselsnummer"].asText() !in sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "GJENLEVENDE_FORELDER" }
            .map { it["foedselsnummer"].asText() }
    }

    private fun personErIkkeDoed(person: Person): Boolean {
        return person.doedsdato.isNullOrEmpty()
    }

    private fun harVerge(sok: JsonMessage): Boolean {
        return sok["@skjema_info"]["soeker"]["verge"]["svar"].asText() == "JA"
    }

    private fun harHuketAvForUtenlandsopphold(sok: JsonMessage): Boolean {
        return sok["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "AVDOED" }
            .filter { it["utenlandsopphold"]["svar"].asText() == "JA" }
            .isNotEmpty()
    }

    //TODO tenke litt mer på dette kriteriet
    private fun ikkeGyldigBostedsAdresseINorge(person: Person): Boolean {
        return person.adresse?.bostedsadresse?.vegadresse?.adressenavn == null
    }

    data class FordelRespons(
        val kandidat: Boolean,
        val forklaring: List<String>
    )

    class Kriterie(val forklaring: String, private val sjekk: Sjekk) {
        fun blirOppfyltAv(message: JsonMessage): Boolean = sjekk(message)
    }

    private fun fordel(packet: JsonMessage): FordelRespons {
        return kriterier
            .filter { it.blirOppfyltAv(packet) }
            .map { it.forklaring }
            .let { FordelRespons(it.isEmpty(), it) }
    }
}
typealias Sjekk = (JsonMessage) -> Boolean