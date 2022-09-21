package barnepensjon.domain

import no.nav.etterlatte.libs.common.behandling.ManueltOpphoerAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak

data class Aarsak(
    val manueltOpphoerFritekstgrunn: String?,
    val manueltOpphoerKjenteGrunner: List<ManueltOpphoerAarsak> = emptyList(),
    val revurderingAarsak: RevurderingAarsak?
)