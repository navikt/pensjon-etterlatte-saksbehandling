package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

class DoedshendelseJobService(
    private val doedshendelseDao: DoedshendelseDao,
    private val doedshendelseKontrollpunktService: DoedshendelseKontrollpunktService,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val dagerGamleHendelserSomSkalKjoeres: Int,
    private val behandlingService: BehandlingService,
    private val pdlTjenesterKlient: PdlTjenesterKlient,
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
                    "Avbryter behandling av dødshendelse for person ${doedshendelse.avdoedFnr.maskerFnr()} med avdød " +
                        "${doedshendelse.avdoedFnr.maskerFnr()} grunnet kontrollpunkt: " +
                        kontrollpunkter.joinToString(","),
                )
                doedshendelseDao.oppdaterDoedshendelse(doedshendelse.tilAvbrutt())
            }

            false -> {
                grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(
                    fnr = doedshendelse.avdoedFnr,
                    grunnlagendringType = GrunnlagsendringsType.DOEDSFALL,
                )
                // todo: EY-3539 - lukk etter oppgave er opprettet
            }
        }
    }

    private fun skalSendeBrevBP(
        doedshendelse: Doedshendelse,
        sak: Sak,
    ) {
        if (doedshendelse.relasjon == Relasjon.BARN) {
            /*
              sjekk om har barnepensjon
             */
            val hentSisteIverksatte = behandlingService.hentSisteIverksatte(sak.id)
            val harIkkeBarnepensjon = hentSisteIverksatte == null
            if (harIkkeBarnepensjon) {
                // TODO: sjekk om har uføretrygd

                // TODO: sjekk om begge foreldre er døde
                val pdlPerson = pdlTjenesterKlient.hentPdlModell(doedshendelse.beroertFnr, PersonRolle.BARN, sak.sakType)
                val foreldre = pdlPerson.familieRelasjon?.verdi?.foreldre ?: emptyList()
                val foreldreMedData =
                    foreldre.map {
                        val rolle = if (doedshendelse.avdoedFnr == it.value) PersonRolle.AVDOED else PersonRolle.GJENLEVENDE
                        pdlTjenesterKlient.hentPdlModell(it.value, rolle, sak.sakType)
                    }
                beggeForeldreErDoede(foreldreMedData)
                // -> må sende brev
            }
        }
    }

    private fun beggeForeldreErDoede(foreldre: List<PersonDTO>): Boolean {
        return foreldre.all { it.doedsdato != null }
    }

    private fun finnGyldigeDoedshendelser(hendelser: List<Doedshendelse>): List<Doedshendelse> {
        val idag = LocalDateTime.now()
        return hendelser.filter {
            Duration.between(it.endret, idag.toTidspunkt()).toDays() >= dagerGamleHendelserSomSkalKjoeres
        }.distinctBy { it.avdoedFnr }.also { logger.info("Antall gyldige dødsmeldinger ${it.size}") }
    }

    private fun hentAlleNyeDoedsmeldinger() = doedshendelseDao.hentDoedshendelserMedStatus(Status.NY)
}
