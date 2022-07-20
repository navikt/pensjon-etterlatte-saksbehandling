package no.nav.etterlatte.fordeler


import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.fordeler.FordelerResultat.*
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

data class FordelerEvent(
    val soeknad: Barnepensjon,
    val hendelseGyldigTil: OffsetDateTime,
)
internal class Fordeler(
    rapidsConnection: RapidsConnection,
    private val fordelerService: FordelerService,
    private val fordelerMetricLogger: FordelerMetricLogger = FordelerMetricLogger()
) : River.PacketListener {


    private val logger = LoggerFactory.getLogger(Fordeler::class.java)

    init {
        River(rapidsConnection).apply{
            eventName("soeknad_innsendt")
            validate { it.demandValue("@skjema_info.type", "BARNEPENSJON") }
            validate { it.demandValue("@skjema_info.versjon", "2") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("@template") }
            validate { it.requireKey("@lagret_soeknad_id") }
            validate { it.requireKey("@hendelse_gyldig_til") }
            validate { it.requireKey("@adressebeskyttelse") }
            validate { it.requireKey("@fnr_soeker") }
            validate { it.rejectKey("@soeknad_fordelt") }
            validate { it.rejectKey("@dokarkivRetur") }
            correlationId()
        }.register(this)

    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId()) {
            try {
                logger.info("Sjekker om soknad (${packet.soeknadId()}) er gyldig for fordeling")

                when (val resultat = fordelerService.sjekkGyldighetForBehandling(packet.toFordelerEvent())) {
                    is GyldigForBehandling -> {
                        logger.info("Soknad ${packet.soeknadId()} er gyldig for fordeling")
                        context.publish(packet.leggPaaFordeltStatus().toJson())
                        fordelerMetricLogger.logMetricFordelt()
                    }
                    is IkkeGyldigForBehandling -> {
                        logger.info("Avbrutt fordeling: ${resultat.ikkeOppfylteKriterier}")
                        fordelerMetricLogger.logMetricIkkeFordelt(resultat)
                    }
                    is UgyldigHendelse -> {
                        logger.error("Avbrutt fordeling: ${resultat.message}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Uhåndtert feilsituasjon: ${e.message}", e)
            }
        }

    private fun JsonMessage.toFordelerEvent() =
        FordelerEvent(
            soeknad = get("@skjema_info").toJson().let(objectMapper::readValue),
            hendelseGyldigTil = get("@hendelse_gyldig_til").textValue().let(OffsetDateTime::parse)
        )

    private fun JsonMessage.leggPaaFordeltStatus(): JsonMessage {
             this["@soeknad_fordelt"] = true
             eventName = "FORDELER:FORDELT"
             correlationId = getCorrelationId()
             return this
        }

    private fun JsonMessage.soeknadId(): Int = get("@lagret_soeknad_id").intValue()
}
