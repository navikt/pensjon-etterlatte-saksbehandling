package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime

class DoedshendelseJobService(
    private val doedshendelseDao: DoedshendelseDao,
    private val featureToggleService: FeatureToggleService,
    private val grunnlagsendringshendelseService: GrunnlagsendringshendelseService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun run() {
        if (featureToggleService.isEnabled(DoedshendelseFeatureToggle.KanLagreDoedshendelse, false)) {
            val nyeDoedshendelser = hentAlleNyeDoedsmeldinger()
            logger.info("Antall nye dødsmeldinger ${nyeDoedshendelser.size}")
            hendelserErGyldige(nyeDoedshendelser).forEach {
                grunnlagsendringshendelseService.opprettHendelseAvTypeForPerson(it.avdoedFnr, GrunnlagsendringsType.DOEDSFALL)
            }
        }
    }

    internal fun hendelserErGyldige(hendelser: List<Doedshendelse>): List<Doedshendelse> {
        val idag = LocalDateTime.now()
        val treDagerSiden = idag.minusDays(3L).toTidspunkt()
        return hendelser.filter {
            val opprettetFor3dagerSiden = it.opprettet.toLocalDatetimeUTC().minusDays(3L)
            opprettetFor3dagerSiden.isBefore(
                treDagerSiden.toLocalDatetimeUTC(),
            ) || Duration.between(it.opprettet, idag.toTidspunkt()).toDays() > 3L
        }.also { logger.info("Antall gyldige dødsmeldinger ${it.size}") }
    }

    private fun hentAlleNyeDoedsmeldinger() = doedshendelseDao.hentDoedshendelserMedStatus(DoedshendelseStatus.NY)
}
