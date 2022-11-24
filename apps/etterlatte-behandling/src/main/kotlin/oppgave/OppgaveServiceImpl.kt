package no.nav.etterlatte.oppgave

import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.GrunnlagsendringsOppgave

interface OppgaveService {

    fun finnOppgaverForBruker(bruker: Saksbehandler): List<Oppgave>
    fun finnOppgaverUhaandterteGrunnlagsendringshendelser(bruker: Saksbehandler): List<GrunnlagsendringsOppgave>
}

class OppgaveServiceImpl(private val oppgaveDao: OppgaveDao) : OppgaveService {

    private fun finnAktuelleRoller(bruker: Saksbehandler): List<Rolle> =
        listOfNotNull(
            Rolle.SAKSBEHANDLER.takeIf { bruker.harRolleSaksbehandler() },
            Rolle.ATTESTANT.takeIf { bruker.harRolleAttestant() }
        )

    private fun aktuelleStatuser(roller: List<Rolle>) = roller.flatMap {
        when (it) {
            Rolle.SAKSBEHANDLER -> listOf(
                BehandlingStatus.UNDER_BEHANDLING,
                BehandlingStatus.GYLDIG_SOEKNAD,
                BehandlingStatus.RETURNERT
            )

            Rolle.ATTESTANT -> listOf(BehandlingStatus.FATTET_VEDTAK)
        }
    }.distinct()

    override fun finnOppgaverForBruker(bruker: Saksbehandler): List<Oppgave> {
        val rollerSomBrukerHar = finnAktuelleRoller(bruker)
        val aktuelleStatuserForRoller = aktuelleStatuser(rollerSomBrukerHar)

        return oppgaveDao.finnOppgaverMedStatuser(aktuelleStatuserForRoller)
    }

    override fun finnOppgaverUhaandterteGrunnlagsendringshendelser(
        _bruker: Saksbehandler
    ): List<GrunnlagsendringsOppgave> {
        return oppgaveDao.finnOppgaverFraGrunnlagsendringshendelser()
    }
}