package no.nav.etterlatte.libs.common.grunnlag

import no.nav.etterlatte.libs.common.FoedselsnummerDTO
import no.nav.etterlatte.libs.common.behandling.Persongalleri

data class PersongalleriRequest(
    val sakId: Long,
    val fnr: FoedselsnummerDTO,
    val persongalleri: Persongalleri
)