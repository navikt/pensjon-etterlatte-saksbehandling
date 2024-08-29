package no.nav.etterlatte.libs.common.grunnlag

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.SakId

data class Opplysningsbehov(
    val sakId: SakId,
    val sakType: SakType,
    val persongalleri: Persongalleri,
    val kilde: Grunnlagsopplysning.Kilde? = null,
)
