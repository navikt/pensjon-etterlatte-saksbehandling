package no.nav.etterlatte.regulering

import no.nav.etterlatte.libs.common.sak.Saker
import org.slf4j.Logger
import java.time.Duration
import kotlin.math.max
import kotlin.math.min

const val MAKS_BATCHSTOERRELSE = 100

fun kjoerIBatch(
    logger: Logger,
    finnSaker: (Int) -> Saker,
    antall: Int,
    haandterSaker: (Saker) -> Unit,
) {
    val maksBatchstoerrelse = MAKS_BATCHSTOERRELSE
    var tatt = 0

    while (tatt < antall) {
        val antallIDenneRunden = max(0, min(maksBatchstoerrelse, antall - tatt))
        logger.info("Starter å ta $antallIDenneRunden av totalt $antall saker")

        val sakerTilOmregning = finnSaker(antallIDenneRunden)

        logger.info("Henta ${sakerTilOmregning.saker.size} saker")

        if (sakerTilOmregning.saker.isEmpty()) {
            logger.debug("Ingen saker i denne runden. Returnerer")
            return
        }

        haandterSaker(sakerTilOmregning)

        tatt += sakerTilOmregning.saker.size
        logger.info("Ferdig med $tatt av totalt $antall saker")
        if (sakerTilOmregning.saker.size < maksBatchstoerrelse) {
            return
        }
        val venteperiode = Duration.ofSeconds(5)
        logger.info("Venter $venteperiode før neste runde.")
        Thread.sleep(venteperiode)
    }
}
