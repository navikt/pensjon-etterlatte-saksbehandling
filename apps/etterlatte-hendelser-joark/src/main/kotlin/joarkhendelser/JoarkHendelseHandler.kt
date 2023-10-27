package no.nav.etterlatte.joarkhendelser

import joarkhendelser.behandling.BehandlingKlient
import joarkhendelser.common.JournalfoeringHendelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JoarkHendelseHandler(
    private val behandlingKlient: BehandlingKlient,
) {
    private val logger: Logger = LoggerFactory.getLogger(JoarkHendelseHandler::class.java)

    suspend fun haandterHendelse(hendelse: JournalfoeringHendelse) {
        if (!hendelse.erTemaEtterlatte()) {
            logger.info("Hendelse (id=${hendelse.hendelsesId}) har tema ${hendelse.temaNytt} og håndteres ikke")
            return
        }

        logger.info("Mottok joarkhendelse med tema ${hendelse.temaNytt}. Starter behandling...")

        try {
            // TODO:
            //  - Hent journalpost fra Saf for å finne FNR
            //  - Sjekk om bruker har sak på tema
            //      - JA: Koble journalpost til saken automatisk (opprette oppgave...?)
            //      - NEI: Opprett oppgave til saksbehandler for manuell behandling
        } catch (e: Exception) {
            throw e
        }
    }
}
