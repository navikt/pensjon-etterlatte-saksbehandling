package no.nav.etterlatte.libs.common.beregning
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
    REVURDERING,
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
    val grunnlagVerson: Long = 0
)

data class Beregningsperiode(
    val delytelsesId: String,
    val type: Beregningstyper,
    val datoFOM: YearMonth,
    val datoTOM: YearMonth?,
    val belop: Int
)

