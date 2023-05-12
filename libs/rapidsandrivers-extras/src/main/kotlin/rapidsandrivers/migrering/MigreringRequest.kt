package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class MigreringRequest(
    val pesysId: PesysId,
    val enhet: Enhet,
    val fnr: Folkeregisteridentifikator,
    val mottattDato: LocalDateTime,
    val persongalleri: Persongalleri,
    val virkningstidspunkt: YearMonth,
    val trygdetidperioder: Trygdetidperioder
)

data class PesysId(val id: String)

data class Enhet(val nr: String)
data class Trygdetidperioder(val perioder: List<DatoPeriode>)

data class DatoPeriode(val fom: LocalDate, val tom: LocalDate)