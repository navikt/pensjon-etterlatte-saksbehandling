package no.nav.etterlatte.oppgave.kommentar

import no.nav.etterlatte.libs.common.oppgave.OppgaveKommentarDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class OppgaveKommentarService(
    private val oppgaveKommentarDao: OppgaveKommentarDao,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    fun hentOppgaveKommentarer(oppgaveId: UUID): List<OppgaveKommentarDto> {
        return oppgaveKommentarDao.hentKommentarer(oppgaveId) // TODO sortert?
    }

    fun opprettKommentar(oppgaveKommentarDto: OppgaveKommentarDto) {
        oppgaveKommentarDao.opprettKommentar(oppgaveKommentarDto)
    }
}
