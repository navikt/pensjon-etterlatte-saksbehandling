package no.nav.etterlatte.oppgave.kommentar

import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.oppgave.OppgaveKommentarDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKommentarRequest
import no.nav.etterlatte.libs.common.oppgave.OppgaveSaksbehandler
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class OppgaveKommentarService(
    private val oppgaveKommentarDao: OppgaveKommentarDao,
    private val oppgaveService: OppgaveService,
    private val sakLesDao: SakLesDao,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    fun hentOppgaveKommentarer(oppgaveId: UUID): List<OppgaveKommentarDto> = oppgaveKommentarDao.hentKommentarer(oppgaveId)

    fun opprettKommentar(
        request: OppgaveKommentarRequest,
        oppgaveId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val sak = sakLesDao.hentSak(oppgave.sakId) ?: throw InternfeilException("Fant ikke sak med id ${oppgave.sakId}")

        logger.info("Oppretter kommentar for oppgave=$oppgaveId sak=${sak.id}")
        val oppgaveKommentar =
            OppgaveKommentarDto(
                sakId = sak.id,
                oppgaveId = oppgaveId,
                saksbehandler = OppgaveSaksbehandler(brukerTokenInfo.ident()),
                kommentar = request.kommentar,
                tidspunkt = Tidspunkt.now(),
            )

        oppgaveKommentarDao.opprettKommentar(oppgaveKommentar)
    }
}
