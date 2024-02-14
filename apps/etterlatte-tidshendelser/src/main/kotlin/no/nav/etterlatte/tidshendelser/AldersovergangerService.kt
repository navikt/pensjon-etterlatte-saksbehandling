package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.slf4j.LoggerFactory

class AldersovergangerService(
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(AldersovergangerService::class.java)

    fun execute(jobb: HendelserJobb) {
        logger.info("Handling jobb ${jobb.id} med type ${jobb.type} (${jobb.type.beskrivelse})")
        hendelseDao.oppdaterJobbstatusStartet(jobb)

        val yearsToSubtract =
            when (jobb.type) {
                JobbType.AO_BP18 -> 18L
                JobbType.AO_BP20 -> 20L
                JobbType.AO_BP21 -> 21L
            }

        val foedselsmaaned = jobb.behandlingsmaaned.minusYears(yearsToSubtract)

        val saker = grunnlagKlient.hentSaker(foedselsmaaned = foedselsmaaned)
        logger.info("Hentet ${saker.size} saker for brukere født i $foedselsmaaned")

        if (saker.isNotEmpty()) {
            hendelseDao.opprettHendelserForSaker(jobb.id, saker, Steg.IDENTIFISERT_SAK)
        } else {
            // Nuttin' to do
            hendelseDao.hentJobb(jobb.id).also {
                hendelseDao.oppdaterJobbstatusFerdig(it)
            }
        }
    }
}
