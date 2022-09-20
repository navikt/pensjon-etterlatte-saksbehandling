package barnepensjon.domain

import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak

data class Aarsak(
    val manueltOpphoerFritekst: String?,
    val manueltOpphoerListe: List<ManueltOpphoerAarsak> = emptyList(),
    val revurderingAarsak: RevurderingAarsak?
)