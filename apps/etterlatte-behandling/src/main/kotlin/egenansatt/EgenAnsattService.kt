package no.nav.etterlatte.egenansatt

import no.nav.etterlatte.sak.SakService

class EgenAnsattService(private val sakService: SakService) {

    fun haandterSkjerming(skjermetHendelse: SkjermetHendelse) {
        val finnSaker = sakService.finnSaker(skjermetHendelse.fnr)
        finnSaker
            .map { it.id }
            .also {
                if (it.isNotEmpty()) {
                    sakService.markerSakerMedSkjerming(it, skjermetHendelse.skjermet)
                }
            }
    }
}