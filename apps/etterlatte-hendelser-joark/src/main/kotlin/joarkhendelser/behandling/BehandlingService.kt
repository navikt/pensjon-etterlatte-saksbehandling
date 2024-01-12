package no.nav.etterlatte.joarkhendelser.behandling

import joarkhendelser.behandling.BehandlingKlient
import joarkhendelser.pdl.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.maskerFnr
import org.slf4j.LoggerFactory
import java.util.UUID

class BehandlingService(
    private val behandlingKlient: BehandlingKlient,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun hentSak(
        ident: String,
        sakType: SakType,
    ): Long? {
        logger.info("Henter sak ($sakType) for bruker=${ident.maskerFnr()}")
        return behandlingKlient.hentSak(ident, sakType)
    }

    suspend fun opprettOppgave(
        ident: String,
        sakType: SakType,
        merknad: String,
        journalpostId: String,
    ): UUID {
        val sakId = hentEllerOpprettSak(ident, sakType)

        logger.info("Oppretter journalføringsoppgave for sak=$sakId")

        return behandlingKlient.opprettOppgave(sakId, merknad, referanse = journalpostId)
            .also { oppgaveId ->
                logger.info("Opprettet oppgave=$oppgaveId med sakId=$sakId for journalpost=$journalpostId")
            }
    }

    private suspend fun hentEllerOpprettSak(
        ident: String,
        sakType: SakType,
    ): Long {
        val gradering = pdlTjenesterKlient.hentAdressebeskyttelse(ident)
        logger.info("Bruker=${ident.maskerFnr()} har gradering $gradering")

        logger.info("Henter/oppretter sak av type=${sakType.name.lowercase()} for bruker=${ident.maskerFnr()} med gradering=$gradering")
        return behandlingKlient.hentEllerOpprettSak(ident, sakType, gradering)
    }
}
