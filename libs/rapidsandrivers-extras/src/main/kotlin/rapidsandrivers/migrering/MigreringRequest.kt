package rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.LocalDate

data class MigreringRequest(
    val pesysId: PesysId,
    val fnr: Folkeregisteridentifikator,
    val mottattDato: LocalDate,
    val persongalleri: Persongalleri
)

data class PesysId(val id: String)