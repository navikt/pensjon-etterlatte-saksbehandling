package no.nav.etterlatte.egenansatt

import no.nav.etterlatte.behandling.BrukerService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.grunnlagsendring.SakMedEnhet
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EgenAnsattService(
    private val sakService: SakService,
    private val oppgaveService: OppgaveService,
    val sikkerLogg: Logger,
    private val brukerService: BrukerService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun haandterSkjerming(skjermetHendelse: EgenAnsattSkjermet) {
        val saker = sakService.finnSaker(skjermetHendelse.fnr)
        val sakerMedGradering = sakService.sjekkOmSakerErGradert(saker.map(Sak::id))
        val sakerSomHarGradering = sakerMedGradering.filter { it.adressebeskyttelseGradering?.erGradert() ?: false }
        if (sakerSomHarGradering.isNotEmpty()) return
        val maskerFnr = skjermetHendelse.fnr.maskerFnr()
        if (saker.isEmpty()) {
            logger.debug(
                "Fant ingen saker for skjermethendelse fnr: $maskerFnr " +
                    "se sikkerlogg og/eller skjekk korrelasjonsid. Selvom dette var forventet," +
                    " ellers hadde ikke vi fått denne hendelsen",
            )
            sikkerLogg.debug("Fant ingen ingen saker for fnr: ${skjermetHendelse.fnr}")
        }

        val sakerMedNyEnhet =
            saker.map {
                SakMedEnhet(
                    it.id,
                    if (skjermetHendelse.skjermet) {
                        Enheter.EGNE_ANSATTE.enhetNr
                    } else {
                        brukerService
                            .finnEnhetForPersonOgTema(
                                skjermetHendelse.fnr,
                                it.sakType.tema,
                                it.sakType,
                            ).enhetNr
                    },
                )
            }
        sakService.oppdaterEnhetForSaker(sakerMedNyEnhet)
        oppgaveService.oppdaterEnhetForRelaterteOppgaver(sakerMedNyEnhet)
        sakService.markerSakerMedSkjerming(saker.map { it.id }, skjermetHendelse.skjermet)
        logger.info(
            "Oppdaterte sakider: ${saker.joinToString(
                separator = ",",
            ) { it.id.toString() }} antall saker med ny enhet(skjerming) for fnr: $maskerFnr , skjermet: ${skjermetHendelse.skjermet}",
        )
    }
}
