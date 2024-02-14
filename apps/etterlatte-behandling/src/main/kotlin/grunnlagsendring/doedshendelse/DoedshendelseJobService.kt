package no.nav.etterlatte.grunnlagsendring.doedshendelse

import no.nav.etterlatte.behandling.domain.GrunnlagsendringsType
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
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

    private fun hendelserErGyldige(hendelser: List<Doedshendelse>): List<Doedshendelse> {
        val idag = LocalDateTime.now()
        return hendelser.filter {
            Duration.between(it.endret, idag.toTidspunkt()).toDays() >= 2L
        }.distinctBy { it.avdoedFnr }.also { logger.info("Antall gyldige dødsmeldinger ${it.size}") }
    }

    private fun hentAlleNyeDoedsmeldinger() = doedshendelseDao.hentDoedshendelserMedStatus(DoedshendelseStatus.NY)
}
