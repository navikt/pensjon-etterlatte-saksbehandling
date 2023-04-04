package rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.LocalDateTime

data class MigreringRequest(
    val pesysId: PesysId,
    val enhet: Enhet,
    val fnr: Folkeregisteridentifikator,
    val mottattDato: LocalDateTime,
    val persongalleri: Persongalleri
)

data class PesysId(val id: String)

data class Enhet(val nr: String)