package no.nav.etterlatte.grunnlagsendring.doedshendelse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.common.DatabaseContext
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.grunnlagsendring.doedshendelse.kontrollpunkt.DoedshendelseKontrollpunktService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import javax.sql.DataSource

class DoedshendelseJobService(
    private val doedshendelseDao: DoedshendelseDao,
    private val doedshendelseKontrollpunktService: DoedshendelseKontrollpunktService,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
    private val dagerGamleHendelserSomSkalKjoeres: Int,
    dataSource: DataSource,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var jobContext: Context

    init {
        jobContext = Context(Self(this::class.java.simpleName), DatabaseContext(dataSource))
    }

    suspend fun run() {
        withContext(Dispatchers.Default + Kontekst.asContextElement(jobContext)) {
            println("withContext   'runBlocking': I'm working in thread ${Thread.currentThread().name}")
            if (true) {
                inTransaction {
                    val nyeDoedshendelser = hentAlleNyeDoedsmeldinger()
                    logger.info("Antall nye dødsmeldinger ${nyeDoedshendelser.size}")

                    val doedshendelserSomSkalHaanderes = finnGyldigeDoedshendelser(nyeDoedshendelser)
                    logger.info("Antall dødsmeldinger plukket ut for kjøring: ${doedshendelserSomSkalHaanderes.size}")

                    doedshendelserSomSkalHaanderes.forEach { doedshendelse ->
                        logger.info("Starter håndtering av dødshendelse for person ${doedshendelse.beroertFnr.maskerFnr()}")
                        haandterDoedshendelse(doedshendelse)
                    }
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

    private fun finnGyldigeDoedshendelser(hendelser: List<Doedshendelse>): List<Doedshendelse> {
        val idag = LocalDateTime.now()
        return hendelser.filter {
            Duration.between(it.endret, idag.toTidspunkt()).toDays() >= dagerGamleHendelserSomSkalKjoeres
        }.distinctBy { it.avdoedFnr }.also { logger.info("Antall gyldige dødsmeldinger ${it.size}") }
    }

    private fun hentAlleNyeDoedsmeldinger() = doedshendelseDao.hentDoedshendelserMedStatus(Status.NY)
}
