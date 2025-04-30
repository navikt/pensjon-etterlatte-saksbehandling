package no.nav.etterlatte.tidshendelser.omregning

import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.rapidsandrivers.RapidEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringHendelseType
import no.nav.etterlatte.rapidsandrivers.tilSeparertString
import no.nav.etterlatte.tidshendelser.hendelser.HendelserJobb
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

private fun kjoering(konfigurasjon: Omregningskonfigurasjon) =
    when (konfigurasjon.kjoeringId) {
        null -> "Regulering-${konfigurasjon.dato.year}"
        else -> "Regulering-${konfigurasjon.dato.year}-${konfigurasjon.kjoeringId}"
    }

class ReguleringService(
    private val rapidsPublisher: (UUID, String) -> Unit,
    private val omregningDao: OmregningDao,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")
        val konfigurasjon = omregningDao.hentNyesteKonfigurasjon()
        when (jobb.type) {
            JobbType.REGULERING -> startRegulering(konfigurasjon)
            JobbType.FINN_SAKER_TIL_REGULERING -> finnSakerTilRegulering(konfigurasjon)
            else -> throw IllegalArgumentException("Ikke-støttet jobbtype: ${jobb.type}")
        }
        return emptyList()
    }

    private fun startRegulering(konfigurasjon: Omregningskonfigurasjon) {
        logger.info("StartReguleringJob startet")
        rapidsPublisher(
            UUID.randomUUID(),
            createRecord(konfigurasjon),
        )
        logger.info("StartReguleringJob ferdig")
    }

    private fun finnSakerTilRegulering(konfigurasjon: Omregningskonfigurasjon) {
        logger.info("Finner saker til regulering startet")
        rapidsPublisher(
            UUID.randomUUID(),
            JsonMessage
                .newMessage(
                    mapOf(
                        ReguleringHendelseType.FINN_SAKER_TIL_REGULERING.lagParMedEventNameKey(),
                        ReguleringEvents.DATO to konfigurasjon.dato.toString(),
                        RapidEvents.KJOERING to kjoering(konfigurasjon),
                    ),
                ).toJson(),
        )
        logger.info("Finner saker til regulering ferdig")
    }
}

fun createRecord(konfigurasjon: Omregningskonfigurasjon) =
    JsonMessage
        .newMessage(
            mapOf(
                ReguleringHendelseType.REGULERING_STARTA.lagParMedEventNameKey(),
                ReguleringEvents.DATO to konfigurasjon.dato.toString(),
                RapidEvents.KJOERING to kjoering(konfigurasjon),
                RapidEvents.ANTALL to konfigurasjon.antall,
                RapidEvents.SPESIFIKKE_SAKER to konfigurasjon.spesifikkeSaker.tilSeparertString(),
                RapidEvents.EKSKLUDERTE_SAKER to konfigurasjon.ekskluderteSaker.tilSeparertString(),
            ),
        ).toJson()
