package no.nav.etterlatte.tidshendelser.etteroppgjoer

import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.tidshendelser.hendelser.HendelserJobb
import no.nav.etterlatte.tidshendelser.klient.BehandlingKlient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EtteroppgjoerService(
    private val etteroppgjoerDao: EtteroppgjoerDao,
    private val behandlingKlient: BehandlingKlient,
) {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<Long> {
        logger.info("Handling jobb ${jobb.id}, type ${jobb.type} (${jobb.type.beskrivelse})")

        val etteroppgjoerKonfigurasjon = etteroppgjoerDao.hentNyesteKonfigurasjon()

        when (jobb.type) {
            JobbType.OPPRETT_ETTEROPPGJOER_FORBEHANDLING -> startOpprettelseAvEtteroppgjoerForbehandling(etteroppgjoerKonfigurasjon)
            else -> throw IllegalArgumentException("Ikke-støttet jobbtype: ${jobb.type}")
        }

        return emptyList()
    }

    private fun startOpprettelseAvEtteroppgjoerForbehandling(etteroppgjoerKonfigurasjon: EtteroppgjoerKonfigurasjon) {
        logger.info("Start opprettelse av etteroppgjoer job startet")

        // TODO logikk for behandlings klient som ber behandling om å opprette etteroppgjør forbehandlinger
        behandlingKlient.toString()
        logger.info("Start opprettelse av etteroppgjoer job startet")
    }
}
