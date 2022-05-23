package no.nav.etterlatte.libs.common.avkorting
import no.nav.etterlatte.libs.common.beregning.Beregningstyper
import java.time.LocalDateTime
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
    val type: Beregningstyper,
    val datoFOM: LocalDateTime,
    val datoTOM: LocalDateTime,
    val belop: Int
)

