package rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.behandling.Persongalleri

data class MigreringRequest(
    val pesysId: PesysId,
    val fnr: String,
    val mottattDato: String,
    val persongalleri: Persongalleri
)

data class PesysId(val id: String)