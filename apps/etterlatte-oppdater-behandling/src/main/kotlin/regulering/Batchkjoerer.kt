package no.nav.etterlatte.regulering

import no.nav.etterlatte.libs.common.sak.SakslisteDTO
import org.slf4j.Logger
import java.time.Duration
import kotlin.math.max
import kotlin.math.min

const val MAKS_BATCHSTOERRELSE = 100

fun kjoerIBatch(
    logger: Logger,
    finnSaker: (Int) -> SakslisteDTO,
    antall: Int,
    haandterSaker: (SakslisteDTO) -> Unit,
    venteperiode: Duration = Duration.ofSeconds(5),
) {
    val maksBatchstoerrelse = MAKS_BATCHSTOERRELSE
    var tatt = 0

    while (tatt < antall) {
        val antallIDenneRunden = max(0, min(maksBatchstoerrelse, antall - tatt))
        logger.info("Starter å ta $antallIDenneRunden av totalt $antall saker")

        val sakerTilOmregning = finnSaker(antallIDenneRunden)

        logger.info("Henta ${sakerTilOmregning.sakIdListe.size} saker")

        if (sakerTilOmregning.sakIdListe.isEmpty()) {
            logger.debug("Ingen saker i denne runden. Returnerer")
            return
        }

        haandterSaker(sakerTilOmregning)

        tatt += sakerTilOmregning.sakIdListe.size
        logger.info("Ferdig med $tatt av totalt $antall saker")
        if (sakerTilOmregning.sakIdListe.size < maksBatchstoerrelse) {
            return
        }
        logger.info("Venter $venteperiode før neste runde.")
        Thread.sleep(venteperiode)
    }
}
