package no.nav.etterlatte.tidshendelser.oppgave

import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.tidshendelser.hendelser.HendelserJobb
import org.slf4j.LoggerFactory

class OppfoelgingsOppgaveService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun execute(jobb: HendelserJobb): List<SakId> {
        // TODO: oppfølgingsoppgave unntak som utløper

        return emptyList()
    }
}
