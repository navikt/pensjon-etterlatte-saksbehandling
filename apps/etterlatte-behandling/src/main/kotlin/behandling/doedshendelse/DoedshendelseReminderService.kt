package no.nav.etterlatte.behandling.doedshendelse

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseFeatureToggle
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseInternal
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
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
        if (featureToggleService.isEnabled(DoedshendelseFeatureToggle.KanLagreDoedshendelse, false)) {
            val hentAlleFerdigDoedsmeldingerMedBrevBp = hentAlleFerdigDoedsmeldingerMedBrevBp()
            val toMaanedGamlehendelser = hendelserErGamleNok(hentAlleFerdigDoedsmeldingerMedBrevBp)
            toMaanedGamlehendelser.forEach { lagOppgaveOmIkkeSoekt(it) }
        }
    }

    private fun lagOppgaveOmIkkeSoekt(hendelse: DoedshendelseInternal) {
        val behandlingerForSak = behandlingService.hentBehandlingerForSak(hendelse.sakId!!)
        val harSoekt = behandlingerForSak.any { it is Foerstegangsbehandling }
        if (!harSoekt) {
            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = hendelse.id.toString(),
                sakId = hendelse.sakId,
                oppgaveKilde = OppgaveKilde.HENDELSE,
                oppgaveType = OppgaveType.VURDER_KONSEKVENS,
                merknad = "${hendelse.beroertFnr} Har ikke søkt om Barnepensjon 2 måneder etter utsendt brev",
                frist = Tidspunkt.now().plus(30L, ChronoUnit.DAYS),
            )
        }
    }

    private fun hendelserErGamleNok(hendelser: List<DoedshendelseInternal>): List<DoedshendelseInternal> {
        val idag = LocalDateTime.now().plusHours(1L)
        val toMaaneder = 2L
        return hendelser.filter { ChronoUnit.MONTHS.between(it.endret.toLocalDatetimeNorskTid(), idag).absoluteValue >= toMaaneder }
    }

    private fun hentAlleFerdigDoedsmeldingerMedBrevBp() = doedshendelseDao.hentDoedshendelserMedStatusFerdigOgUtFallBrevBp()
}
