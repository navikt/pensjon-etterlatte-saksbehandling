package no.nav.etterlatte.libs.common.grunnlag

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType

data class Opplysningsbehov(
    val sakId: no.nav.etterlatte.libs.common.sak.SakId,
    val sakType: SakType,
    val persongalleri: Persongalleri,
    val kilde: Grunnlagsopplysning.Kilde? = null,
)
