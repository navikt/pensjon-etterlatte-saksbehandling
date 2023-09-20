package no.nav.etterlatte.egenansatt

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.GrunnlagsendringshendelseService
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.sak.SakService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EgenAnsattService(private val sakService: SakService, val sikkerLogg: Logger) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun haandterSkjerming(skjermetHendelse: EgenAnsattSkjermet) {
        val saker = sakService.finnSaker(skjermetHendelse.fnr)
        val sakerMedGradering = sakService.sjekkOmSakerErGradert(saker.map(Sak::id))
        val sakerSomHarGradering = sakerMedGradering.filter { it.adressebeskyttelseGradering?.erGradert() ?: false }
        if (sakerSomHarGradering.isNotEmpty()) return
        if (saker.isEmpty()) {
            logger.debug(
                "Fant ingen saker for skjermethendelse fnr: ${skjermetHendelse.fnr.maskerFnr()} " +
                    "se sikkerlogg og/eller skjekk korrelasjonsid. Selvom dette var forventet," +
                    " ellers hadde ikke vi fått denne hendelsen",
            )
            sikkerLogg.debug("Fant ingen ingen saker for fnr: ${skjermetHendelse.fnr}")
        }

        val sakerMedNyEnhet =
            saker.map {
                GrunnlagsendringshendelseService.SakMedEnhet(
                    it.id,
                    if (skjermetHendelse.skjermet) {
                        Enheter.EGNE_ANSATTE.enhetNr
                    } else {
                        sakService.finnEnhetForPersonOgTema(
                            skjermetHendelse.fnr,
                            it.sakType.tema,
                            it.sakType,
                        ).enhetNr
                    },
                )
            }
        sakService.oppdaterEnhetForSaker(sakerMedNyEnhet)
        sakService.markerSakerMedSkjerming(saker.map { it.id }, skjermetHendelse.skjermet)
    }
}
