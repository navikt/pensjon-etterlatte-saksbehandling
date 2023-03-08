package no.nav.etterlatte.oppgave

import no.nav.etterlatte.Saksbehandler
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.oppgave.domain.Oppgave

interface OppgaveService {

    fun finnOppgaverForBruker(bruker: Saksbehandler): List<Oppgave>
}

class OppgaveServiceImpl(private val oppgaveDao: OppgaveDao) : OppgaveService {

    private fun finnAktuelleRoller(bruker: Saksbehandler): List<Rolle> =
        listOfNotNull(
            Rolle.SAKSBEHANDLER.takeIf { bruker.harRolleSaksbehandler() },
            Rolle.ATTESTANT.takeIf { bruker.harRolleAttestant() }
        )

    private fun aktuelleStatuserForRolleTilSaksbehandler(roller: List<Rolle>) = roller.flatMap {
        when (it) {
            Rolle.SAKSBEHANDLER -> BehandlingStatus.kanEndres()
            Rolle.ATTESTANT -> listOf(BehandlingStatus.FATTET_VEDTAK)
            Rolle.STRENGT_FORTROLIG -> BehandlingStatus.values().toList()
        }.distinct()
    }

    override fun finnOppgaverForBruker(bruker: Saksbehandler): List<Oppgave> {
        val rollerSomBrukerHar = finnAktuelleRoller(bruker)
        val aktuelleStatuserForRoller = aktuelleStatuserForRolleTilSaksbehandler(rollerSomBrukerHar)

        return if (bruker.harRolleStrengtFortrolig()) {
            inTransaction {
                oppgaveDao.finnOppgaverForStrengtFortroligOgStrengtFortroligUtland(aktuelleStatuserForRoller)
            }
        } else {
            inTransaction {
                oppgaveDao.finnOppgaverMedStatuser(aktuelleStatuserForRoller) +
                    oppgaveDao.finnOppgaverFraGrunnlagsendringshendelser()
            }.sortedByDescending { it.registrertDato }
        }
    }
}