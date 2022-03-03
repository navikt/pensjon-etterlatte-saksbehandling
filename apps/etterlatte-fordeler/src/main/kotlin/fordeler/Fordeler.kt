package no.nav.etterlatte.fordeler


import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
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

private data class FordelerEvent(
    val soeknad: Barnepensjon,
    val soeknadId: String,
    val hendelseGyldigTil: OffsetDateTime,
)

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
                val event: FordelerEvent = packet.toFordelerEvent()
                val soeknad: Barnepensjon = event.soeknad

                logger.info("Sjekker om soknad (${event.soeknadId}) er gyldig for fordeling")

                if (ugyldigHendelse(event))
                    throw FordelingAvbruttException(
                        "Avbrutt fordeling: Hendelsen er ikke lenger gyldig (${hendelseGyldigTil(event)})",
                        severity = ERROR
                    )

                if (ugyldigSoeknadstype(soeknad))
                    throw FordelingAvbruttException(
                        "Avbrutt fordeling: Søknad er ikke barnepensjon (${soeknad.type})",
                        severity = INFO
                    )

                runBlocking {
                    val barn = pdlTjenesterKlient.hentPerson(hentBarnRequest(soeknad))
                    val avdoed = pdlTjenesterKlient.hentPerson(hentAvdoedRequest(soeknad))
                    val gjenlevende = pdlTjenesterKlient.hentPerson(hentGjenlevendeRequest(soeknad))

                    fordelerKriterierService.sjekkMotKriterier(barn, avdoed, gjenlevende, soeknad).let {
                        if (it.kandidat) {
                            logger.info("Soknad ${event.soeknadId} er gyldig for fordeling")
                            context.publish(packet.toFordeltEvent().toJson())
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

    private fun ugyldigSoeknadstype(soeknad: Barnepensjon) =
        soeknad.type != SoeknadType.BARNEPENSJON

    private fun ugyldigHendelse(event: FordelerEvent) =
        event.hendelseGyldigTil.isBefore(OffsetDateTime.now(klokke))

    private fun hendelseGyldigTil(event: FordelerEvent) =
        event.hendelseGyldigTil.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    private fun hentGjenlevendeRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.foreldre.first { it.type == PersonType.GJENLEVENDE_FORELDER }.foedselsnummer.svar,
            rolle = PersonRolle.GJENLEVENDE
        )

    private fun hentAvdoedRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.foreldre.first { it.type == PersonType.AVDOED }.foedselsnummer.svar,
            rolle = PersonRolle.AVDOED
        )

    private fun hentBarnRequest(soeknad: Barnepensjon) =
        HentPersonRequest(
            foedselsnummer = soeknad.soeker.foedselsnummer.svar,
            rolle = PersonRolle.BARN
        )

    private fun JsonMessage.toFordeltEvent() =
        apply {
            this["@soeknad_fordelt"] = true
            this["@event_name"] = "ey_fordelt"
            this["@correlation_id"] = getCorrelationId()
        }

    private fun JsonMessage.toFordelerEvent() =
        FordelerEvent(
            soeknad = get("@skjema_info").textValue().let(objectMapper::readValue),
            soeknadId = get("@lagret_soeknad_id").textValue(),
            hendelseGyldigTil = get("@hendelse_gyldig_til").textValue().let(OffsetDateTime::parse)
        )

    private fun JsonMessage.correlationId(): String? = get("@correlation_id").textValue()
}
