package no.nav.etterlatte.oppgaveny

import org.slf4j.LoggerFactory
import java.sql.Connection

class OppgaveDaoNy(private val connection: () -> Connection) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun finnOppgaverForBruker(): List<OppgaveNy> {
    }
}