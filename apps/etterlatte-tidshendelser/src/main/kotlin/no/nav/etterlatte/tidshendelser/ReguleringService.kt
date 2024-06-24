package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.Month
import java.util.UUID

val year = LocalDate.now().year
val GRUNNBELOEP_REGULERING_DATO: LocalDate = LocalDate.of(year, Month.MAY, 1)
private val kjoering = "Regulering-$GRUNNBELOEP_REGULERING_DATO"

class ReguleringService(
    private val rapidsPublisher: (UUID, String) -> Unit,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")
        when (jobb.type) {
            JobbType.REGULERING -> startRegulering()
            JobbType.FINN_SAKER_TIL_REGULERING -> finnSakerTilRegulering()
            else -> throw IllegalArgumentException("Ikke-støttet jobbtype: ${jobb.type}")
        }
        return listOf()
    }

    private fun startRegulering() {
        logger.info("StartReguleringJob startet")
        rapidsPublisher(
            UUID.randomUUID(),
            createRecord(GRUNNBELOEP_REGULERING_DATO),
        )
        logger.info("StartReguleringJob ferdig")
    }

    private fun finnSakerTilRegulering() {
        logger.info("Finner saker til regulering startet")
        rapidsPublisher(
            UUID.randomUUID(),
            finnSakerTilRegulering(GRUNNBELOEP_REGULERING_DATO),
        )
        logger.info("Finner saker til regulering ferdig")
    }
}

fun createRecord(dato: LocalDate) =
    JsonMessage
        .newMessage(
            mapOf(
                ReguleringHendelseType.REGULERING_STARTA.lagParMedEventNameKey(),
                ReguleringEvents.DATO to dato.toString(),
                ReguleringEvents.KJOERING to kjoering,
                ReguleringEvents.ANTALL to 20,
            ),
        ).toJson()

fun finnSakerTilRegulering(dato: LocalDate) =
    JsonMessage
        .newMessage(
            mapOf(
                ReguleringHendelseType.FINN_SAKER_TIL_REGULERING.lagParMedEventNameKey(),
                ReguleringEvents.DATO to dato.toString(),
                ReguleringEvents.KJOERING to kjoering,
            ),
        ).toJson()
