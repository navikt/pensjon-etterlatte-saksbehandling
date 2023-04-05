package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.rapidsandrivers.migrering.Enhet
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId
import java.time.LocalDateTime

data class Pesyssak(
    val id: PesysId,
    val enhet: Enhet,
    val folkeregisteridentifikator: Folkeregisteridentifikator,
    val mottattdato: LocalDateTime,
    val persongalleri: Persongalleri
)