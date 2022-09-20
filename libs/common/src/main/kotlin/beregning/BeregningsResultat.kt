package no.nav.etterlatte.libs.common.beregning

import no.nav.etterlatte.libs.common.person.Person
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

enum class Beregningstyper {
    GP,
    GBBP,
    BPGP,
    BIPIDYBOPIDY
}

enum class Endringskode {
    NY,
    REVURDERING
}

enum class BeregningsResultatType {
    BEREGNET, FAARIKKEPAENG;
}

data class BeregningsResultat(
    val id: UUID,
    val type: Beregningstyper,
    val endringskode: Endringskode,
    val resultat: BeregningsResultatType,
    val beregningsperioder: List<Beregningsperiode>,
    val beregnetDato: LocalDateTime,
    val grunnlagVersjon: Long = 0L
)

data class Beregningsperiode(
    val delytelsesId: String,
    val type: Beregningstyper,
    val datoFOM: YearMonth,
    val datoTOM: YearMonth?,
    val utbetaltBeloep: Int,
    val soeskenFlokk: List<Person>?,
    val grunnbelopMnd: Int,
    val grunnbelop: Int
)

data class SoeskenPeriode(
    val datoFOM: YearMonth,
    val datoTOM: YearMonth,
    val soeskenFlokk: List<Person>
)

fun SoeskenPeriode.erInklusiv(FOM: YearMonth, TOM: YearMonth) =
    !FOM.isBefore(this.datoFOM) && !TOM.isAfter(this.datoTOM)