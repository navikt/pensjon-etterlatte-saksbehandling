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
import no.nav.etterlatte.sak.SakLesDao
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

class DoedshendelseReminderService(
    private val doedshendelseDao: DoedshendelseDao,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val sakLesDao: SakLesDao,
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
        logger.info("Starter dødshendelsejob for å sjekke om vi har meldinger som er gamle nok")
        val alleFerdigDoedsmeldingerMedBrevBp = inTransaction { hentAlleFerdigDoedsmeldingerMedBrevBp() }
        logger.info("Antall meldinger på vent ${alleFerdigDoedsmeldingerMedBrevBp.size}")
        val gamleNokHendelser = hendelserErGamleNok(alleFerdigDoedsmeldingerMedBrevBp)
        logger.info("Antall hendelser som er gamle nok hendelser ${alleFerdigDoedsmeldingerMedBrevBp.size}")
        gamleNokHendelser.forEach {
            inTransaction { lagOppgaveOmIkkeSoekt(it) }
        }
    }

    private fun lagOppgaveOmIkkeSoekt(hendelse: DoedshendelseReminder) {
        logger.info("Behandler doedshendelseReminder id ${hendelse.id} med sakid: ${hendelse.sakId}")
        if (hendelse.sakId == null) {
            logger.info(
                "Kan ikke opprette oppfølgingsoppgave for hendelse uten sakId. " +
                    "Trolig fordi det er en gammel hendelse som ikke har sakId pga. feature toggle. HendelseId: ${hendelse.id}",
            )
            return
        }
        val sak = sakLesDao.hentSak(hendelse.sakId)
        if (sak == null) {
            logger.error(
                "Hendelse med id ${hendelse.id} refererer til en sak med id=${hendelse.sakId} som ikke fins " +
                    "i Gjenny. Vi kan dermed ikke følge opp om de har søkt. Setter sakId for hendelsen til null. " +
                    "Dette bør i prinsippet ikke skje i produksjon, siden vi ikke sletter saker der uten videre. " +
                    "Hvis dette har skjedd i produksjon bør det følges opp for å se om det er riktig.",
            )
            val doedshendelse = doedshendelseDao.hentDoedshendelseMedId(hendelse.id)
            doedshendelseDao.oppdaterDoedshendelse(
                doedshendelse.copy(
                    sakId = null,
                ),
            )
            return
        }
        val behandlingerForSak = behandlingService.hentBehandlingerForSak(hendelse.sakId)
        val harSoekt = behandlingerForSak.any { it is Foerstegangsbehandling }
        logger.info("Doedshendelse har blitt søkt på $harSoekt")
        if (!harSoekt) {
            val oppgaveFinnes = oppgaveService.oppgaveMedTypeFinnes(hendelse.sakId, OppgaveType.MANGLER_SOEKNAD)
            if (!oppgaveFinnes) {
                logger.info("Oppretter oppgave for dødshende hendelse ${hendelse.id} ${OppgaveType.MANGLER_SOEKNAD}")
                val standardFrist30dager = Tidspunkt.now().plus(30L, ChronoUnit.DAYS)
                oppgaveService.opprettOppgave(
                    referanse = hendelse.id.toString(),
                    sakId = hendelse.sakId,
                    kilde = OppgaveKilde.DOEDSHENDELSE,
                    type = OppgaveType.MANGLER_SOEKNAD,
                    merknad = "Har ikke søkt om barnepensjon $fireMaaneder mnd. etter utsendt brev. Sjekk om det må sendes påminnelse.",
                    frist = standardFrist30dager,
                )
            }
        }
    }

    private fun hendelserErGamleNok(hendelser: List<DoedshendelseReminder>): List<DoedshendelseReminder> {
        val idag = LocalDateTime.now(norskTidssone)
        return hendelser.filter {
            ChronoUnit.MONTHS
                .between(
                    it.endret.toLocalDatetimeNorskTid(),
                    idag,
                ).absoluteValue >= fireMaaneder
        }
    }

    private fun hentAlleFerdigDoedsmeldingerMedBrevBp() = doedshendelseDao.hentDoedshendelserMedStatusFerdigOgUtFallBrevBp()
}
