package no.nav.etterlatte.behandling.etteroppgjoer.pensjonsgivendeinntekt

import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.PensjonsgivendeInntektAarResponse
import no.nav.etterlatte.libs.regler.FaktumNode

class PensjonsgivendeInntektGrunnlag(
    val pensjonsgivendeInntektAarResponse: FaktumNode<PensjonsgivendeInntektAarResponse>,
    val aar: FaktumNode<Int>,
)
