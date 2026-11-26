package no.nav.etterlatte.behandling.revurdering

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException

class ForlengetOmsUnderUtvikling :
    UgyldigForespoerselException(
        "FORLENGET_OMS_UNDER_UTVIKLING",
        "Forlenget OMS kan ikke ferdigstilles før nye aktivitets- og vilkårsregler og vedtaksbrev er på plass.",
    )

fun Behandling.validerForlengetOmsPeriode() {
    if (this !is Revurdering || revurderingsaarsak != Revurderingaarsak.FORLENGET_OMS) {
        return
    }

    val periode = revurderingInfo?.revurderingInfo as? RevurderingInfo.ForlengetOms
    if (periode == null ||
        periode.tom < periode.fom ||
        opphoerFraOgMed != periode.tom.plusMonths(1)
    ) {
        throw UgyldigForespoerselException(
            "UGYLDIG_FORLENGET_OMS_PERIODE",
            "Forlenget OMS må ha en gyldig lagret periode og opphør fra måneden etter siste stønadsmåned.",
        )
    }
}
