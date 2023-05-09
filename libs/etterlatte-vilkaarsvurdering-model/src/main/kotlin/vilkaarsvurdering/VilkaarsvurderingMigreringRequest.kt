package no.nav.etterlatte.vilkaarsvurdering

import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import java.util.*

data class VilkaarsvurderingMigreringRequest(
    val sakId: Long,
    val behandlingId: UUID,
    val fnr: FoedselsnummerDTO,
    val persongalleri: Persongalleri
)