package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.slf4j.LoggerFactory

class AldersovergangerService(
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(AldersovergangerService::class.java)

    suspend fun runJob(jobb: HendelserJobb) {
        if (jobb.type == JobType.AO_BP20) {
            val foedselsmaaned = jobb.behandlingsmaaned.minusYears(20)

            val saker = grunnlagKlient.hentSakerForBrukereFoedtIMaaned(foedselsmaaned)
            logger.info("Hentet ${saker.size} saker for brukere født i $foedselsmaaned")

            hendelseDao.opprettHendelserForSaker(jobb.id, saker)

            // Do something with saker
        }
    }
}
