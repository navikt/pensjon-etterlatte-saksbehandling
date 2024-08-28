package no.nav.etterlatte.tidshendelser.regulering

import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.tidshendelser.HendelserJobb
import no.nav.etterlatte.tidshendelser.JobbType
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rapidsandrivers.RapidEvents
import rapidsandrivers.tilSeparertString
import java.time.LocalDate
import java.util.UUID

private fun kjoering(dato: LocalDate) = "Regulering-$dato"

class ReguleringService(
    private val rapidsPublisher: (UUID, String) -> Unit,
    private val reguleringDao: ReguleringDao,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")
        val konfigurasjon = reguleringDao.hentNyesteKonfigurasjon()
        when (jobb.type) {
            JobbType.REGULERING -> startRegulering(konfigurasjon)
            JobbType.FINN_SAKER_TIL_REGULERING -> finnSakerTilRegulering(konfigurasjon)
            else -> throw IllegalArgumentException("Ikke-støttet jobbtype: ${jobb.type}")
        }
        return listOf()
    }

    private fun startRegulering(konfigurasjon: Reguleringskonfigurasjon) {
        logger.info("StartReguleringJob startet")
        rapidsPublisher(
            UUID.randomUUID(),
            createRecord(konfigurasjon),
        )
        logger.info("StartReguleringJob ferdig")
    }

    private fun finnSakerTilRegulering(konfigurasjon: Reguleringskonfigurasjon) {
        logger.info("Finner saker til regulering startet")
        rapidsPublisher(
            UUID.randomUUID(),
            JsonMessage
                .newMessage(
                    mapOf(
                        ReguleringHendelseType.FINN_SAKER_TIL_REGULERING.lagParMedEventNameKey(),
                        ReguleringEvents.DATO to konfigurasjon.dato.toString(),
                        RapidEvents.KJOERING to kjoering(konfigurasjon.dato),
                    ),
                ).toJson(),
        )
        logger.info("Finner saker til regulering ferdig")
    }
}

fun createRecord(konfigurasjon: Reguleringskonfigurasjon) =
    JsonMessage
        .newMessage(
            mapOf(
                ReguleringHendelseType.REGULERING_STARTA.lagParMedEventNameKey(),
                ReguleringEvents.DATO to konfigurasjon.dato.toString(),
                RapidEvents.KJOERING to kjoering(konfigurasjon.dato),
                RapidEvents.ANTALL to konfigurasjon.antall,
                RapidEvents.SPESIFIKKE_SAKER to konfigurasjon.spesifikkeSaker.tilSeparertString(),
                RapidEvents.EKSKLUDERTE_SAKER to konfigurasjon.ekskluderteSaker.tilSeparertString(),
            ),
        ).toJson()
