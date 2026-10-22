package no.nav.etterlatte.beregning

import no.nav.etterlatte.beregning.grunnlag.Vedtaksperiode
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException

internal fun List<Vedtaksperiode>.validerKunEnVedtaksperiode() {
    if (size > 1) {
        throw UgyldigForespoerselException(
            "FLERPERIODEBEREGNING_IKKE_AKTIV",
            "Beregning med flere innvilgede perioder er ikke aktivert.",
        )
    }
}
