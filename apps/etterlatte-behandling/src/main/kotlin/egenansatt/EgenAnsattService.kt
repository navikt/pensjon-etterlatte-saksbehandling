package no.nav.etterlatte.egenansatt

import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.sak.SakService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EgenAnsattService(private val sakService: SakService, val sikkerLogg: Logger) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    fun haandterSkjerming(skjermetHendelse: EgenAnsattSkjermet) {
        val saker = sakService.finnSaker(skjermetHendelse.fnr)
        if (saker.isEmpty()) {
            logger.error(
                "Fant ingen saker for skjermethendelse fnr: ${skjermetHendelse.fnr.maskerFnr()} " +
                    "se sikkerlogg og/eller skjekk korrelasjonsid"
            )
            sikkerLogg.info("Fant ingen ingen saker for fnr: ${skjermetHendelse.fnr}")
        }
        val sakerMedNyEnhet = saker.map {
            GrunnlagsendringshendelseService.SakMedEnhet(
                it.id,
                sakService.finnEnhetForPersonOgTema(
                    skjermetHendelse.fnr,
                    it.sakType.tema
                ).enhetNr
            )
        }
        sakService.oppdaterEnhetForSaker(sakerMedNyEnhet)
        sakService.markerSakerMedSkjerming(saker.map { it.id }, skjermetHendelse.skjermet)
    }
}