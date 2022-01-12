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
    private val personService : PersonService,
    private val klokke: Clock = Clock.systemUTC()
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(EtterlatteFordeler::class.java)
    private lateinit var barn: Person
    //TODO mangler kriterier

    //Avdød:
    //Bodd i Norge hele livet
    //Helst ikke ha en annen ytelse i forkant - f.eks ufør og alderspensjon. (Da er det allerede gjort en trygdetidsvurdering)
    //Ikke huket av for yrkesskade/yrkessykdom
    //Dødsfallet må ha skjedd i Norge og være registrert. Må få en dødsdato fra PDL (ikke være noe som er uavklart rundt dette)

    //Barn:
    //Født og oppvokst i Norge
    // (v) Alder: under 15 år (slik at vi har nok tid til det blir en eventuell omberegning når barnet er 18 år)
    //Muligens ikke ta hensyn til barn som "er på vei" - enda ikke født
    //Ett barn, ingen søsken
    //Må vi ta høyde for flere søsken
    //Kan vi tenke på kun fellesbarn i første versjon?

    //Innsender av søknad:
    //Gjenlevende forelder
    //Hva gjør vi med verge? Høre med Randi Aasen om tanker og hva som er gjort tidligere med vergemål/fullmaktløsning.
    //Mulighet for å se fra PDL foreldreansvar og om det er oppnevnt verge 

    val kriterier = listOf(
        Kriterie("Barn er ikke norsk statsborger") { sjekkStatsborgerskapBarn() },
        Kriterie("Barn er for gammelt") { barnForGammel() },
        Kriterie("Barn har adressebeskyttelse") { barnHarAdressebeskyttelse() },
        Kriterie("Barn ikke fodt i Norge") { barnFoedtUtland() },
        Kriterie("Barn har utvandring") { barnHarUtvandring() }
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
        //TODO må denne skrives om til å håndtere manglende soeknads_type?
        if(packet["@skjema_info"]["type"].asText() != SoeknadType.Barnepensjon.name.uppercase())
        {
            logger.info("Avbrutt fordeling da søknad ikke er " + SoeknadType.Barnepensjon.name)
            return
        }

        runBlocking {

            try {
                val barnFnr = Foedselsnummer.of(packet["@fnr_soeker"].asText())
                barn = personService.hentPerson(barnFnr)
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

    private fun barnHarAdressebeskyttelse(): Boolean {
        return barn.adressebeskyttelse
    }

    private fun barnForGammel(): Boolean {
        return barn.alder() > 15
    }

    private fun sjekkStatsborgerskapBarn(): Boolean {
        return barn.statsborgerskap != "NOR"
    }
    private fun barnFoedtUtland(): Boolean {
        println(barn.foedeland)
        return barn.foedeland != "NOR"
    }
    private fun barnHarUtvandring(): Boolean {
        //TODO endre til sjekk av utvandring
        return barn.foedeland != "NOR"
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