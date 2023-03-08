package no.nav.etterlatte.egenansatt

import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.skjermet.EgenAnsattSkjermet
import no.nav.etterlatte.sak.SakService
import org.slf4j.LoggerFactory

class EgenAnsattService(private val sakService: SakService) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    fun haandterSkjerming(skjermetHendelse: EgenAnsattSkjermet) {
        sakService.finnSaker(skjermetHendelse.fnr).map { it.id }
            .also {
                if (it.isNotEmpty()) {
                    logger.info(
                        "Fant saker for skjermethendelse fnr: ${skjermetHendelse.fnr.maskerFnr()} " +
                            "saker: ${it.joinToString(",")}"
                    )
                    sakService.markerSakerMedSkjerming(it, skjermetHendelse.skjermet)
                } else {
                    logger.info("Fant ingen saker for skjermethendelse fnr: ${skjermetHendelse.fnr.maskerFnr()}")
                }
            }
    }
}