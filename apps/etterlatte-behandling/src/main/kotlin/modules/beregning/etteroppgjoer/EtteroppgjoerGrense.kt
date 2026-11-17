package no.nav.etterlatte.modules.beregning.etteroppgjoer

import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerGrenseDto
import no.nav.etterlatte.regler.Beregningstall
import java.time.LocalDate

data class EtteroppgjoerGrense(
    val tilbakekreving: Beregningstall,
    val etterbetaling: Beregningstall,
    val rettsgebyr: EtteroppgjoerRettsgebyr,
) {
    fun toDto(): EtteroppgjoerGrenseDto =
        EtteroppgjoerGrenseDto(
            tilbakekreving = this.tilbakekreving.toDouble(),
            etterbetaling = this.etterbetaling.toDouble(),
            rettsgebyr = this.rettsgebyr.rettsgebyr.toInteger(),
            rettsgebyrGyldigFra = this.rettsgebyr.gyldigFra,
        )
}

data class EtteroppgjoerRettsgebyr(
    val gyldigFra: LocalDate,
    val rettsgebyr: Beregningstall,
)
