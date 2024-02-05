package no.nav.etterlatte.tidshendelser

import org.slf4j.LoggerFactory

class JobbRunner(
    private val hendelseDao: HendelseDao,
    private val aldersovergangerService: AldersovergangerService,
) {
    private val logger = LoggerFactory.getLogger(JobbRunner::class.java)

    fun run() {
        logger.info("Sjekker for jobber å starte...")

        hendelseDao.hentJobber("NY").forEach { jobb ->
            logger.info("Fant jobb ${jobb.id} med type ${jobb.type}, status (${jobb.status})")

//            when (jobb.type) {
//                JobbType.AO_BP20 -> aldersovergangerService.execute(jobb)
//            }
        }
    }
}
