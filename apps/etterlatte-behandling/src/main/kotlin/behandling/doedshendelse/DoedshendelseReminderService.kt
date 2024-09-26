package no.nav.etterlatte.behandling.doedshendelse

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.grunnlagsendring.doedshendelse.DoedshendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskTidssone
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

class DoedshendelseReminderService(
    private val doedshendelseDao: DoedshendelseDao,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private val fireMaaneder = 4L
    }

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        val alleFerdigDoedsmeldingerMedBrevBp = inTransaction { hentAlleFerdigDoedsmeldingerMedBrevBp() }
        val toMaanedGamlehendelser = hendelserErGamleNok(alleFerdigDoedsmeldingerMedBrevBp)
        toMaanedGamlehendelser.forEach {
            inTransaction { lagOppgaveOmIkkeSoekt(it) }
        }
    }

    private fun lagOppgaveOmIkkeSoekt(hendelse: DoedshendelseReminder) {
        if (hendelse.sakId == null) {
            logger.info(
                "Kan ikke opprette oppfølgingsoppgave for hendelse uten sakId. " +
                    "Trolig fordi det er en gammel hendelse som ikke har sakId pga. feature toggle. HendelseId: ${hendelse.id}",
            )
            return
        }
        val behandlingerForSak = behandlingService.hentBehandlingerForSak(hendelse.sakId)
        val harSoekt = behandlingerForSak.any { it is Foerstegangsbehandling }
        if (!harSoekt) {
            val oppgaver = oppgaveService.hentOppgaverForSak(hendelse.sakId, OppgaveType.MANGLER_SOEKNAD)
            if (oppgaver.isEmpty()) {
                oppgaveService.opprettOppgave(
                    referanse = hendelse.id.toString(),
                    sakId = hendelse.sakId,
                    kilde = OppgaveKilde.DOEDSHENDELSE,
                    type = OppgaveType.MANGLER_SOEKNAD,
                    merknad = "Har ikke søkt om barnepensjon 2 mnd. etter utsendt brev. Sjekk om det må sendes påminnelse.",
                    frist = Tidspunkt.now().plus(30L, ChronoUnit.DAYS),
                )
            }
        }
    }

    private fun hendelserErGamleNok(hendelser: List<DoedshendelseReminder>): List<DoedshendelseReminder> {
        val idag = LocalDateTime.now(norskTidssone)
        return hendelser.filter { ChronoUnit.MONTHS.between(it.endret.toLocalDatetimeNorskTid(), idag).absoluteValue >= fireMaaneder }
    }

    private fun hentAlleFerdigDoedsmeldingerMedBrevBp() = doedshendelseDao.hentDoedshendelserMedStatusFerdigOgUtFallBrevBp()
}
