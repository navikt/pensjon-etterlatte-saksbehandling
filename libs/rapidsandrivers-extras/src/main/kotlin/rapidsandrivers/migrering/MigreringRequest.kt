package no.nav.etterlatte.rapidsandrivers.migrering

import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.math.BigDecimal
import java.time.YearMonth

data class MigreringRequest(
    val pesysId: PesysId,
    val enhet: Enhet,
    val soeker: Folkeregisteridentifikator,
    val gjenlevendeForelder: Folkeregisteridentifikator?,
    val avdoedForelder: List<AvdoedForelder>,
    val virkningstidspunkt: YearMonth,
    val foersteVirkningstidspunkt: YearMonth,
    val beregning: Beregning,
    val trygdetid: Trygdetid,
    val flyktningStatus: Boolean = false
) {
    fun opprettPersongalleri() = Persongalleri(
        soeker = this.soeker.value,
        avdoed = this.avdoedForelder.map { it.ident.value },
        gjenlevende = listOfNotNull(this.gjenlevendeForelder?.value)
    )
}

data class AvdoedForelder(
    val ident: Folkeregisteridentifikator,
    val doedsdato: Tidspunkt,
    val yrkesskade: Boolean = false
)

data class PesysId(val id: Long)

data class Enhet(val nr: String)

data class Beregning(
    val brutto: BigDecimal,
    val netto: BigDecimal,
    val anvendtTrygdetid: BigDecimal,
    val datoVirkFom: Tidspunkt,
    val g: BigDecimal,
    val meta: BeregningMeta? = null
)

data class Trygdetid(
    val perioder: List<Trygdetidsgrunnlag>
)

data class BeregningMeta(
    val resultatType: String,
    val beregningsMetodeType: String,
    val resultatKilde: String,
    val kravVelgType: String
)

data class Trygdetidsgrunnlag(
    val trygdetidGrunnlagId: Long,
    val personGrunnlagId: Long,
    val landTreBokstaver: String,
    val datoFom: Tidspunkt,
    val datoTom: Tidspunkt,
    val poengIInnAar: Boolean,
    val poengIUtAar: Boolean,
    val ikkeIProrata: Boolean,
    val faktiskTrygdetid: BigDecimal,
    val fremtidigTrygdetid: BigDecimal,
    val anvendtTrygdetid: BigDecimal
)