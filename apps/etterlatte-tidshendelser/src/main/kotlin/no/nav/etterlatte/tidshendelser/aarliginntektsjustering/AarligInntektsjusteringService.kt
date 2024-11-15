package no.nav.etterlatte.tidshendelser.aarliginntektsjustering

import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.libs.common.rapidsandrivers.lagParMedEventNameKey
import no.nav.etterlatte.rapidsandrivers.InntektsjusteringHendelseType
import no.nav.etterlatte.rapidsandrivers.RapidEvents
import no.nav.etterlatte.rapidsandrivers.ReguleringEvents
import no.nav.etterlatte.rapidsandrivers.tilSeparertString
import no.nav.etterlatte.tidshendelser.HendelserJobb
import no.nav.etterlatte.tidshendelser.regulering.ReguleringDao
import no.nav.etterlatte.tidshendelser.regulering.Reguleringskonfigurasjon
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class AarligInntektsjusteringService(
    private val rapidsPublisher: (UUID, String) -> Unit,
    private val reguleringDao: ReguleringDao,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")
        val konfigurasjon = reguleringDao.hentNyesteKonfigurasjon()
        startAarligInntektsjustering(konfigurasjon)
        return listOf()
    }

    private fun startAarligInntektsjustering(konfigurasjon: Reguleringskonfigurasjon) {
        logger.info("StartInntektsJob startet")
        rapidsPublisher(
            UUID.randomUUID(),
            createRecord(konfigurasjon),
        )
        logger.info("StartInntektsJob ferdig")
    }
}

fun createRecord(konfigurasjon: Reguleringskonfigurasjon) =
    JsonMessage
        .newMessage(
            mapOf(
                InntektsjusteringHendelseType.START_INNTEKTSJUSTERING_JOBB.lagParMedEventNameKey(),
                ReguleringEvents.DATO to AarligInntektsjusteringRequest.utledLoependeFom().atDay(1),
                RapidEvents.KJOERING to AarligInntektsjusteringRequest.utledKjoering(),
                RapidEvents.ANTALL to konfigurasjon.antall,
                RapidEvents.SPESIFIKKE_SAKER to konfigurasjon.spesifikkeSaker.tilSeparertString(),
                RapidEvents.EKSKLUDERTE_SAKER to konfigurasjon.ekskluderteSaker.tilSeparertString(),
            ),
        ).toJson()
