package no.nav.etterlatte.tidshendelser

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
import no.nav.etterlatte.tidshendelser.klient.GrunnlagKlient
import org.slf4j.LoggerFactory

class OmstillingsstoenadService(
    private val hendelseDao: HendelseDao,
    private val grunnlagKlient: GrunnlagKlient,
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")

        val monthsToSubtract: Long =
            when (jobb.type) {
                JobbType.OMS_DOED_3AAR -> 36
                JobbType.OMS_DOED_5AAR -> 60
                JobbType.OMS_DOED_4MND -> 4
                JobbType.OMS_DOED_6MND -> 6
                JobbType.OMS_DOED_6MND_INFORMASJON_VARIG_UNNTAK -> 6
                else -> throw IllegalArgumentException("Ikke-støttet jobbtype: ${jobb.type}")
            }

        val doedsfallsmaaned = jobb.behandlingsmaaned.minusMonths(monthsToSubtract)
        val saker =
            runBlocking {
                retryOgPakkUt {
                    grunnlagKlient.hentSakerForDoedsfall(doedsfallsmaaned = doedsfallsmaaned)
                }
            }

        logger.info("Hentet ${saker.size} saker hvor dødsfall forekom i $doedsfallsmaaned")

        // filtrer bort saker som ikke er aktuelle
        val sakerMap =
            runBlocking {
                retryOgPakkUt {
                    behandlingKlient.hentSaker(saker)
                }
            }
        val aktuelleSaker =
            saker.filter {
                sakerMap[it]?.sakType == jobb.type.sakType
            }
        logger.info(
            "Hentet ${saker.size} saker hvor dødsfall forekom i $doedsfallsmaaned, med " +
                "${aktuelleSaker.size} saker med riktig saktype",
        )

        if (aktuelleSaker.isNotEmpty()) {
            hendelseDao.opprettHendelserForSaker(jobb.id, aktuelleSaker, Steg.IDENTIFISERT_SAK)
        }

        return aktuelleSaker
    }
}
