package no.nav.etterlatte.behandling.doedshendelse

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseFeatureToggle
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.oppgave.OppgaveService
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

class DoedshendelseReminderService(
    private val featureToggleService: FeatureToggleService,
    private val doedshendelseDao: DoedshendelseDao,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {
    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        if (featureToggleService.isEnabled(DoedshendelseFeatureToggle.KanSendeBrevOgOppretteOppgave, false)) {
            val alleFerdigDoedsmeldingerMedBrevBp = inTransaction { hentAlleFerdigDoedsmeldingerMedBrevBp() }
            val toMaanedGamlehendelser = hendelserErGamleNok(alleFerdigDoedsmeldingerMedBrevBp)
            toMaanedGamlehendelser.forEach {
                inTransaction { lagOppgaveOmIkkeSoekt(it) }
            }
        }
    }

    private fun lagOppgaveOmIkkeSoekt(hendelse: DoedshendelseReminder) {
        val behandlingerForSak = behandlingService.hentBehandlingerForSak(hendelse.sakId!!)
        val harSoekt = behandlingerForSak.any { it is Foerstegangsbehandling }
        if (!harSoekt) {
            val oppgaver = oppgaveService.hentOppgaverForSak(hendelse.sakId)
            if (oppgaver.none { it.type == OppgaveType.VURDER_KONSEKVENS }) {
                oppgaveService.opprettOppgave(
                    referanse = hendelse.id.toString(),
                    sakId = hendelse.sakId,
                    kilde = OppgaveKilde.HENDELSE,
                    type = OppgaveType.VURDER_KONSEKVENS,
                    merknad = "${hendelse.beroertFnr} Har ikke søkt om Barnepensjon 2 måneder etter utsendt brev",
                    frist = Tidspunkt.now().plus(30L, ChronoUnit.DAYS),
                )
            }
        }
    }

    private fun hendelserErGamleNok(hendelser: List<DoedshendelseReminder>): List<DoedshendelseReminder> {
        val idag = LocalDateTime.now(norskTidssone)
        val toMaaneder = 2L
        return hendelser.filter { ChronoUnit.MONTHS.between(it.endret.toLocalDatetimeNorskTid(), idag).absoluteValue >= toMaaneder }
    }

    private fun hentAlleFerdigDoedsmeldingerMedBrevBp() = doedshendelseDao.hentDoedshendelserMedStatusFerdigOgUtFallBrevBp()
}
