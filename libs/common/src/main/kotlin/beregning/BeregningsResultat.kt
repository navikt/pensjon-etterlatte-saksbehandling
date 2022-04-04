package no.nav.etterlatte.libs.common.beregning
import java.time.LocalDateTime
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
    val beregnetDato: LocalDateTime
)

data class Beregningsperiode(
    val delytelsesId: String,
    val type: Beregningstyper,
    val datoFOM: LocalDateTime,
    val datoTOM: LocalDateTime,
    val belop: Int
)

