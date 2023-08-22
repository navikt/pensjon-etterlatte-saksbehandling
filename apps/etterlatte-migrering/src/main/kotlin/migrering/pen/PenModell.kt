package migrering.pen

import java.time.LocalDate

data class BarnepensjonGrunnlagResponse(
    val sakId: Long,
    val enhet: String,
    val soeker: String,
    val gjenlevendeForelder: String?,
    val avdoedForeldre: List<AvdoedForelderBP>,
    val virkningsdato: LocalDate,
    val beregning: BeregningBP,
    val trygdetidsgrunnlagListe: List<TrygdetidsgrunnlagBP>,
    val trygdetidListe: List<TrygdetidBP>
)

data class AvdoedForelderBP(
    val ident: String,
    val doedsdato: LocalDate
)

data class BeregningBP(
    val brutto: Int,
    val netto: Int,
    val anvendtTrygdetid: Int,
    val datoVirkFom: LocalDate,
    val g: Int,
    val meta: BeregningMetaBP
)

data class BeregningMetaBP(
    val resultatType: String,
    val beregningsMetodeType: String?,
    val resultatKilde: String,
    val kravVelgType: String
)

data class TrygdetidsgrunnlagBP(
    val trygdetidGrunnlagId: Long,
    val personGrunnlagId: Long,
    val landTreBokstaver: String,
    val datoFom: LocalDate,
    val datoTom: LocalDate,
    val poengIInnAar: Boolean,
    val poengIUtAar: Boolean,
    val ikkeIProrata: Boolean
)

data class TrygdetidBP(
    val faktiskTrygdetid: Int?,
    val fremtidigTrygdetid: Int,
    val anvendtTrygdetid: Int,
    val virkFom: LocalDate,
    val virkTom: LocalDate?
)