package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

data class Pesyssak(
    val id: UUID,
    val pesysId: PesysId,
    val enhet: Enhet,
    val folkeregisteridentifikator: Folkeregisteridentifikator,
    val mottattdato: LocalDateTime,
    val persongalleri: Persongalleri,
    val virkningstidspunkt: YearMonth
)