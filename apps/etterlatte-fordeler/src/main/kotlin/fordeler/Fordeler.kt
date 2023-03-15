package no.nav.etterlatte.fordeler

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.event.FordelerFordelt
import no.nav.etterlatte.libs.common.event.SoeknadInnsendt
import no.nav.etterlatte.libs.common.logging.getCorrelationId
import no.nav.etterlatte.libs.common.logging.withLogContext
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.rapidsandrivers.CORRELATION_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.FEILENDE_KRITERIER_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.GYLDIG_FOR_BEHANDLING_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SAK_TYPE_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.SOEKNAD_ID_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationId
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.rapidsandrivers.EventNames
import no.nav.etterlatte.sikkerLogg
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

data class FordelerEvent(
    val soeknadId: Long,
    val soeknad: Barnepensjon,
    val hendelseGyldigTil: OffsetDateTime
)

internal class Fordeler(
    rapidsConnection: RapidsConnection,
    private val fordelerService: FordelerService,
    private val fordelerMetricLogger: FordelerMetricLogger = FordelerMetricLogger()
) : River.PacketListener {

    private val logger = LoggerFactory.getLogger(Fordeler::class.java)

    init {
        River(rapidsConnection).apply {
            eventName("soeknad_innsendt")
            validate { it.demandValue("@skjema_info.type", "BARNEPENSJON") }
            validate { it.demandValue("@skjema_info.versjon", "2") }
            validate { it.requireKey("@skjema_info") }
            validate { it.requireKey("@template") }
            validate { it.requireKey("@lagret_soeknad_id") }
            validate { it.requireKey("@hendelse_gyldig_til") }
            validate { it.requireKey("@adressebeskyttelse") }
            validate { it.requireKey("@fnr_soeker") }
            validate { it.rejectKey("@dokarkivRetur") }
            validate { it.rejectKey(FordelerFordelt.soeknadFordeltKey) }
            correlationId()
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) =
        withLogContext(packet.correlationId) {
            try {
                logger.info("Sjekker om soknad (${packet.soeknadId()}) er gyldig for fordeling")

                when (val resultat = fordelerService.sjekkGyldighetForBehandling(packet.toFordelerEvent())) {
                    FordelerResultat.GyldigForBehandling -> {
                        logger.info("Soknad ${packet.soeknadId()} er gyldig for fordeling")
                        context.publish(packet.leggPaaFordeltStatus(true).toJson())
                        fordelerMetricLogger.logMetricFordelt()
                        lagStatistikkMelding(packet, resultat, SakType.BARNEPENSJON)
                            ?.let { context.publish(it) }
                    }

                    is FordelerResultat.IkkeGyldigForBehandling -> {
                        logger.info("Avbrutt fordeling: ${resultat.ikkeOppfylteKriterier}")
                        context.publish(packet.leggPaaFordeltStatus(false).toJson())
                        fordelerMetricLogger.logMetricIkkeFordelt(resultat)
                        lagStatistikkMelding(packet, resultat, SakType.BARNEPENSJON)
                            ?.let { context.publish(it) }
                    }

                    is FordelerResultat.UgyldigHendelse -> {
                        logger.warn("Avbrutt fordeling: ${resultat.message}")
                    }
                }
            } catch (e: JsonMappingException) {
                sikkerLogg.error("Feil under deserialisering", e)
                logger.error(
                    "Feil under deserialisering av søknad, soeknadId: ${packet.soeknadId()}" +
                        ". Sjekk sikkerlogg for detaljer."
                )
                throw e
            } catch (e: Exception) {
                logger.error("Uhåndtert feilsituasjon soeknadId: ${packet.soeknadId()}", e)
                throw e
            }
        }

    private fun JsonMessage.toFordelerEvent() =
        FordelerEvent(
            soeknadId = soeknadId(),
            soeknad = get(SoeknadInnsendt.skjemaInfoKey).toJson().let(objectMapper::readValue),
            hendelseGyldigTil = get(SoeknadInnsendt.hendelseGyldigTilKey).textValue().let(OffsetDateTime::parse)
        )

    fun lagStatistikkMelding(packet: JsonMessage, fordelerResultat: FordelerResultat, sakType: SakType): String? {
        val (resultat, ikkeOppfylteKriterier) = when (fordelerResultat) {
            FordelerResultat.GyldigForBehandling -> true to null
            is FordelerResultat.IkkeGyldigForBehandling -> false to fordelerResultat.ikkeOppfylteKriterier
            is FordelerResultat.UgyldigHendelse -> {
                logger.error("Kan ikke produsere statistikkmelding for fordelerResultat $fordelerResultat")
                return null
            }
        }
        val meldingsinnhold: MutableMap<String, Any?> = mutableMapOf(
            CORRELATION_ID_KEY to packet.correlationId,
            EVENT_NAME_KEY to EventNames.FORDELER_STATISTIKK,
            SAK_TYPE_KEY to sakType,
            SOEKNAD_ID_KEY to packet.soeknadId(),
            GYLDIG_FOR_BEHANDLING_KEY to resultat
        )
        ikkeOppfylteKriterier?.let {
            meldingsinnhold[FEILENDE_KRITERIER_KEY] = it
        }
        return meldingsinnhold.toJson()
    }

    private fun JsonMessage.leggPaaFordeltStatus(fordelt: Boolean): JsonMessage {
        this[FordelerFordelt.soeknadFordeltKey] = fordelt
        correlationId = getCorrelationId()
        return this
    }

    private fun JsonMessage.soeknadId(): Long = get(SoeknadInnsendt.lagretSoeknadIdKey).longValue()
}