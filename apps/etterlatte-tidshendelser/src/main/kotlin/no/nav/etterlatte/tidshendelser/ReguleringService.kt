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

class ReguleringService(
    private val rapidsPublisher: (UUID, String) -> Unit,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")
        logger.info("StartReguleringJob startet")
        rapidsPublisher(
            UUID.randomUUID(),
            createRecord(GRUNNBELOEP_REGULERING_DATO),
        )
        logger.info("StartReguleringJob ferdig")
        return listOf()
    }
}

fun createRecord(dato: LocalDate) =
    JsonMessage
        .newMessage(
            mapOf(
                ReguleringHendelseType.REGULERING_STARTA.lagParMedEventNameKey(),
                ReguleringEvents.DATO to dato.toString(),
                ReguleringEvents.KJOERING to "Regulering-$year",
                ReguleringEvents.ANTALL to 20,
                ReguleringEvents.SPESIFIKKE_SAKER to "3482;6323",
            ),
        ).toJson()
