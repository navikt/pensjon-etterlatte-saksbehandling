package no.nav.etterlatte.libs.common.opplysningsbehov

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType

data class Opplysningsbehov(
    val sakid: Long,
    val sakType: SakType,
    val persongalleri: Persongalleri
)