package no.nav.etterlatte.oppgave

import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.sak.SakId
import org.slf4j.LoggerFactory

class OppgaveService(
    private val oppgaveKlient: OppgaveKlient,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun opprettOppgave(
        sakId: SakId,
        nyOppgave: NyOppgaveDto,
    ): OppgaveIntern {
        logger.info("Oppretter ny oppgave for sak $sakId")

        return oppgaveKlient.opprettOppgave(sakId, nyOppgave)
    }
}
