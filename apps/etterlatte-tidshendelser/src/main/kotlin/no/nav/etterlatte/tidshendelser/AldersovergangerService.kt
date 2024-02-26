package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.slf4j.LoggerFactory

class AldersovergangerService(
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(AldersovergangerService::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type=${jobb.type} (${jobb.type.beskrivelse})")

        val yearsToSubtract =
            when (jobb.type) {
                JobbType.AO_BP20 -> 20L
                JobbType.AO_BP21 -> 21L
                JobbType.AO_OMS67 -> 67L
                else -> throw IllegalArgumentException("Ikke-støttet jobbtype: ${jobb.type}")
            }

        val foedselsmaaned = jobb.behandlingsmaaned.minusYears(yearsToSubtract)

        val saker = grunnlagKlient.hentSaker(foedselsmaaned = foedselsmaaned)
        logger.info("Hentet ${saker.size} saker for brukere født i $foedselsmaaned")

        if (saker.isNotEmpty()) {
            hendelseDao.opprettHendelserForSaker(jobb.id, saker, Steg.IDENTIFISERT_SAK)
        }

        return saker
    }
}
