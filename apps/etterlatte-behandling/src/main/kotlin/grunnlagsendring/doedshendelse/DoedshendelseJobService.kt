package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.domain.GrunnlagsendringStatus
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.behandling.domain.Grunnlagsendringshendelse
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

class DoedshendelseJobService(
    private val doedshendelseDao: DoedshendelseDao,
    private val doedshendelseKontrollpunktService: DoedshendelseKontrollpunktService,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val sakService: SakService,
    private val dagerGamleHendelserSomSkalKjoeres: Int,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        if (featureToggleService.isEnabled(DoedshendelseFeatureToggle.KanLagreDoedshendelse, false)) {
            val doedshendelserSomSkalHaanderes =
                inTransaction {
                    val nyeDoedshendelser = hentAlleNyeDoedsmeldinger()
                    logger.info("Antall nye dødsmeldinger ${nyeDoedshendelser.size}")

                    val doedshendelserSomSkalHaanderes = finnGyldigeDoedshendelser(nyeDoedshendelser)
                    logger.info("Antall dødsmeldinger plukket ut for kjøring: ${doedshendelserSomSkalHaanderes.size}")
                    doedshendelserSomSkalHaanderes
                }
            doedshendelserSomSkalHaanderes.forEach { doedshendelse ->
                inTransaction {
                    logger.info("Starter håndtering av dødshendelse for person ${doedshendelse.beroertFnr.maskerFnr()}")
                    haandterDoedshendelse(doedshendelse)
                }
            }
        }
    }

    private fun haandterDoedshendelse(doedshendelse: Doedshendelse) {
        val kontrollpunkter = doedshendelseKontrollpunktService.identifiserKontrollerpunkter(doedshendelse)

        when (kontrollpunkter.any { it.avbryt }) {
            true -> {
                logger.info(
                    "Avbryter behandling av dødshendelse for person ${doedshendelse.beroertFnr.maskerFnr()} med avdød " +
                        "${doedshendelse.avdoedFnr.maskerFnr()} grunnet kontrollpunkt: " +
                        kontrollpunkter.joinToString(","),
                )
                doedshendelseDao.oppdaterDoedshendelse(doedshendelse.tilAvbrutt())
            }

            false -> {
                logger.info(
                    "Oppretter grunnlagshendelse for person ${doedshendelse.beroertFnr.maskerFnr()} " +
                        "med avdød ${doedshendelse.avdoedFnr.maskerFnr()}",
                )
                opprettGrunnlagshendelse(doedshendelse)
            }
        }
    }

    private fun opprettGrunnlagshendelse(doedshendelse: Doedshendelse) {
        val sakType =
            when (doedshendelse.relasjon) {
                Relasjon.BARN -> SakType.BARNEPENSJON
                Relasjon.EPS -> SakType.OMSTILLINGSSTOENAD
            }
        val sak = sakService.finnEllerOpprettSak(doedshendelse.beroertFnr, sakType)

        val opprettetGrunnlagshendelse =
            grunnlagsendringshendelseService.opprettDoedshendelseForPerson(
                Grunnlagsendringshendelse(
                    id = UUID.randomUUID(),
                    sakId = sak.id,
                    status = GrunnlagsendringStatus.SJEKKET_AV_JOBB,
                    type = GrunnlagsendringsType.DOEDSFALL,
                    opprettet = Tidspunkt.now().toLocalDatetimeUTC(),
                    hendelseGjelderRolle = Saksrolle.SOEKER,
                    gjelderPerson = doedshendelse.beroertFnr,
                ),
            )
        doedshendelseDao.oppdaterDoedshendelse(
            doedshendelse.tilBehandlet(
                utfall = Utfall.OPPGAVE,
                sakId = sak.id,
                oppgaveId = opprettetGrunnlagshendelse?.id,
            ),
        )
    }

    private fun finnGyldigeDoedshendelser(hendelser: List<Doedshendelse>): List<Doedshendelse> {
        val idag = LocalDateTime.now()
        return hendelser.filter {
            Duration.between(it.endret, idag.toTidspunkt()).toDays() >= dagerGamleHendelserSomSkalKjoeres
        }.distinctBy { it.avdoedFnr }.also { logger.info("Antall gyldige dødsmeldinger ${it.size}") }
    }

    private fun hentAlleNyeDoedsmeldinger() = doedshendelseDao.hentDoedshendelserMedStatus(Status.NY)
}
