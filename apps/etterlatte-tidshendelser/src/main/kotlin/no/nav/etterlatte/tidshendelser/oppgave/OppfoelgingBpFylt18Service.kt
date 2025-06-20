package no.nav.etterlatte.tidshendelser.oppgave

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import no.nav.etterlatte.tidshendelser.hendelser.HendelserJobb
import no.nav.etterlatte.tidshendelser.hendelser.Steg
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
import org.slf4j.LoggerFactory

class OppfoelgingBpFylt18Service(
    private val hendelseDao: HendelseDao,
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<SakId> {
        logger.info("Handling jobb ${jobb.id}, type=${jobb.type} (${jobb.type.beskrivelse})")

        // TODO rett maaned?
        val utledMaaned = jobb.behandlingsmaaned
        val saker =
            runBlocking {
                retryOgPakkUt {
                    behandlingKlient.hentSakerBpFylt18IMaaned(maaned = utledMaaned)
                }
            }

        logger.info(
            "Hentet ${saker.size} saker for brukere som fyller 18 i $utledMaaned",
        )

        if (saker.isNotEmpty()) {
            hendelseDao.opprettHendelserForSaker(jobb.id, saker, Steg.IDENTIFISERT_SAK)
        }

        return saker
    }
}
