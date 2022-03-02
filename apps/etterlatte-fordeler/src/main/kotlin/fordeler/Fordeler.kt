package no.nav.etterlatte.fordeler


import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.SoeknadType
import no.nav.etterlatte.pdltjenester.PdlTjenesterKlient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.slf4j.event.Level.ERROR
import org.slf4j.event.Level.INFO
import java.time.Clock
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class FordelingAvbruttException(message: String, val severity: Level) : RuntimeException(message)

internal class Fordeler(
    rapidsConnection: RapidsConnection,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
    private val fordelerKriterierService: FordelerKriterierService,
    private val klokke: Clock = Clock.systemUTC()
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(Fordeler::class.java)

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
            validate { it.interestedIn("@correlation_id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                logger.info("Sjekker om soknad (${packet.soknadId()}) er gyldig for fordeling")

                if (packet.hendelseUtgaatt())
                    throw FordelingAvbruttException(
                        "Avbrutt fordeling: Hendelsen er ikke lenger gyldig (${packet.hendelseGyldigTil()})",
                        severity = ERROR
                    )

                if (packet.soknadIkkeBarnepensjon())
                    throw FordelingAvbruttException(
                        "Avbrutt fordeling: Søknad er ikke barnepensjon (${packet.soknadType()})",
                        severity = INFO
                    )

                runBlocking {
                    val barn = pdlTjenesterKlient.hentPerson(hentBarnRequest(packet))
                    val avdoed = pdlTjenesterKlient.hentPerson(hentAvdoedRequest(packet))
                    val gjenlevende = pdlTjenesterKlient.hentPerson(hentGjenlevendeRequest(packet))

                    fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, packet).let {
                        if (it.kandidat) {
                            logger.info("Soknad ${packet.soknadId()} er gyldig for fordeling")
                            context.publish(packet.oppdaterTilFordelt().toJson())
                        } else {
                            throw FordelingAvbruttException("Avbrutt fordeling: ${it.forklaring}", severity = INFO)
                        }
                    }
                }

            } catch (err: FordelingAvbruttException) {
                when (err.severity) {
                    INFO -> logger.info(err.message)
                    else -> logger.error(err.message)
                }
            } catch (err: Exception) {
                logger.error("Uhaandtert feilsituasjon: ${err.message}", err)
            }
        }

    private fun hentGjenlevendeRequest(packet: JsonMessage) =
        HentPersonRequest(packet.gjenlevendeFnr(), PersonRolle.GJENLEVENDE)

    private fun hentAvdoedRequest(packet: JsonMessage) =
        HentPersonRequest(packet.avdoedFnr(), PersonRolle.AVDOED)

    private fun hentBarnRequest(packet: JsonMessage) =
        HentPersonRequest(packet.soekerFnr(), PersonRolle.BARN)

    private fun JsonMessage.soknadId() = this["@lagret_soeknad_id"]

    private fun JsonMessage.soknadType() = this["@skjema_info"]["type"]?.textValue()

    private fun JsonMessage.soknadIkkeBarnepensjon() = this.soknadType() != SoeknadType.BARNEPENSJON.name

    private fun JsonMessage.hendelseGyldigTil() =
        OffsetDateTime.parse(this["@hendelse_gyldig_til"].asText())?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private fun JsonMessage.hendelseUtgaatt() =
        OffsetDateTime.parse(this["@hendelse_gyldig_til"].asText())?.isBefore(OffsetDateTime.now(klokke)) ?: true

    private fun JsonMessage.soekerFnr() = this["@fnr_soeker"].asText().let { Foedselsnummer.of(it) }

    private fun JsonMessage.avdoedFnr(): Foedselsnummer =
        this["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "AVDOED" }
            .map { it["foedselsnummer"].first()}[0].asText()
            .let { Foedselsnummer.of(it) }

    private fun JsonMessage.gjenlevendeFnr() =
        this["@skjema_info"]["foreldre"]
            .filter { it["type"].asText() == "GJENLEVENDE_FORELDER" }
            .map { it["foedselsnummer"].first()}[0].asText()
            .let { Foedselsnummer.of(it) }

    private fun JsonMessage.oppdaterTilFordelt() =
        apply {
            this["@soeknad_fordelt"] = true
            this["@event_name"] = "ey_fordelt"
            this["@correlation_id"] = getCorrelationId()
        }

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}
