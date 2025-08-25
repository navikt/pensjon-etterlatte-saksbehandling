package no.nav.etterlatte.tidshendelser.oppgave

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tidshendelser.hendelser.HendelseDao
import no.nav.etterlatte.tidshendelser.hendelser.HendelserJobb
import no.nav.etterlatte.tidshendelser.hendelser.Steg
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
import org.slf4j.LoggerFactory

class OppdaterSkjermingBpService(
    private val hendelseDao: HendelseDao,
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<SakId> {
        logger.info("Handling jobb ${jobb.id}, type=${jobb.type} (${jobb.type.beskrivelse})")

        val saker =
            runBlocking {
                retryOgPakkUt {
                    behandlingKlient.hentSkjermedeSakerBarnepensjon()
                }
            }

        logger.info(
            "Hentet ${saker.size} skjermede saker for barnepensjon",
        )

        if (saker.isNotEmpty()) {
            hendelseDao.opprettHendelserForSaker(jobb.id, saker, Steg.IDENTIFISERT_SAK)
        }

        return saker
    }
}
