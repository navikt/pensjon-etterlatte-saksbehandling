package no.nav.etterlatte.libs.common.avkorting
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

//TODO denne klassen må gås igjennom
enum class Avkortingstyper {
    INNTEKT,
}

enum class Endringskode {
    NY,
    REVURDERING,
}

enum class AvkortingsResultatType {
    BEREGNET, FAARIKKEPAENG;
}

data class AvkortingsResultat(
    val id: UUID,
    val type: Beregningstyper,
    val endringskode: Endringskode,
    val resultat: AvkortingsResultatType,
    val beregningsperioder: List<Avkortingsperiode>,
    val beregnetDato: LocalDateTime
)

data class Avkortingsperiode(
    val avkortingsId: String,
    val type: Avkortingstyper,
    val datoFOM: YearMonth,
    val datoTOM: YearMonth?,
    val belop: Int
)

