package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.slf4j.LoggerFactory

class OmstillingsstoenadService(
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")

        val monthsToSubtract: Long =
            when (jobb.type) {
                JobbType.OMS_DOED_3AAR -> 36
                JobbType.OMS_DOED_5AAR -> 60
                JobbType.OMS_DOED_4MND -> 4
                else -> throw IllegalArgumentException("Ikke-støttet jobbtype: ${jobb.type}")
            }

        val doedsfallsmaaned = jobb.behandlingsmaaned.minusMonths(monthsToSubtract)
        val saker = grunnlagKlient.hentSakerForDoedsfall(doedsfallsmaaned = doedsfallsmaaned)

        logger.info("Hentet ${saker.size} saker hvor dødsfall forekom i $doedsfallsmaaned")

        if (saker.isNotEmpty()) {
            hendelseDao.opprettHendelserForSaker(jobb.id, saker, Steg.IDENTIFISERT_SAK)
        }

        return saker
    }
}
