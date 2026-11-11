package no.nav.etterlatte.modules.beregning.avkorting

import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import tools.jackson.databind.JsonNode
import java.time.YearMonth
import java.util.UUID

data class Avkorting(
    val aarsoppgjoer: List<Aarsoppgjoer> = emptyList(),
) {
    init {
        if (!aarsoppgjoer.zipWithNext().all { it.first.aar < it.second.aar }) {
            throw InternfeilException("Årsoppgjør er i feil rekkefølge.")
        }
    }
}

sealed class AvkortingGrunnlag {
    abstract val id: UUID
    abstract val periode: Periode
    abstract val innvilgaMaaneder: Int
    abstract val inntektInnvilgetPeriode: InntektInnvilgetPeriode
    abstract val maanederInnvilget: List<MaanedInnvilget>?
    abstract val maanederInnvilgetRegelResultat: JsonNode?
    abstract val spesifikasjon: String
    abstract val kilde: Grunnlagsopplysning.Saksbehandler
}

data class ForventetInntekt(
    override val id: UUID,
    override val periode: Periode,
    val inntektTom: Int,
    val fratrekkInnAar: Int,
    val inntektUtlandTom: Int,
    val fratrekkInnAarUtland: Int,
    override val innvilgaMaaneder: Int,
    override val spesifikasjon: String,
    override val kilde: Grunnlagsopplysning.Saksbehandler,
    val overstyrtInnvilgaMaanederAarsak: OverstyrtInnvilgaMaanederAarsak? = null,
    val overstyrtInnvilgaMaanederBegrunnelse: String? = null,
    override val inntektInnvilgetPeriode: InntektInnvilgetPeriode,
    override val maanederInnvilget: List<MaanedInnvilget>?,
    override val maanederInnvilgetRegelResultat: JsonNode?,
) : AvkortingGrunnlag()

data class FaktiskInntekt(
    override val id: UUID,
    override val periode: Periode,
    override val innvilgaMaaneder: Int,
    val loennsinntekt: Int,
    val naeringsinntekt: Int,
    val afp: Int,
    val utlandsinntekt: Int,
    override val spesifikasjon: String,
    override val kilde: Grunnlagsopplysning.Saksbehandler,
    override val inntektInnvilgetPeriode: BenyttetInntektInnvilgetPeriode,
    override val maanederInnvilget: List<MaanedInnvilget>?,
    override val maanederInnvilgetRegelResultat: JsonNode?,
) : AvkortingGrunnlag()

sealed class InntektInnvilgetPeriode

data class BenyttetInntektInnvilgetPeriode(
    val verdi: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
) : InntektInnvilgetPeriode()

data object IngenInntektInnvilgetPeriode : InntektInnvilgetPeriode()

enum class OverstyrtInnvilgaMaanederAarsak {
    TAR_UT_PENSJON_TIDLIG,
    BLIR_67,
    ANNEN,
}

sealed class Aarsoppgjoer {
    abstract val id: UUID
    abstract val aar: Int
    abstract val fom: YearMonth
    abstract val ytelseFoerAvkorting: List<YtelseFoerAvkorting>
    abstract val avkortetYtelse: List<AvkortetYtelse>
}

data class AarsoppgjoerLoepende(
    override val id: UUID,
    override val aar: Int,
    override val fom: YearMonth,
    override val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntektsavkorting: List<Inntektsavkorting> = emptyList(),
    override val avkortetYtelse: List<AvkortetYtelse> = emptyList(),
) : Aarsoppgjoer() {
    init {
        fun periodeISammeAar(periode: Periode) {
            if (periode.fom.year != aar || (periode.tom?.year?.let { it != aar } == true)) {
                throw InternfeilException("Perioder må være innenfor årsoppgjøret sitt år $periode")
            }
        }

        with(ytelseFoerAvkorting) {
            if (!zipWithNext().all { it.first.periode.fom < it.second.periode.fom }) {
                throw InternfeilException("fom for YtelseFoerAvkorting er i feil rekkefølge")
            }
        }

        with(inntektsavkorting) {
            if (!zipWithNext().all { it.first.grunnlag.periode.fom < it.second.grunnlag.periode.fom }) {
                throw InternfeilException("fom for inntektsavkorting er i feil rekkefølge")
            }
            forEach {
                periodeISammeAar(it.grunnlag.periode)
            }
        }

        with(avkortetYtelse) {
            if (!zipWithNext().all { it.first.periode.fom < it.second.periode.fom }) {
                throw InternfeilException("fom for AvkortetYtelseAar er i feil rekkefølge")
            }
            forEach {
                periodeISammeAar(it.periode)
            }
        }
    }
}

data class Etteroppgjoer(
    override val id: UUID,
    override val aar: Int,
    override val fom: YearMonth,
    override val ytelseFoerAvkorting: List<YtelseFoerAvkorting> = emptyList(),
    val inntekt: FaktiskInntekt,
    val avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    override val avkortetYtelse: List<AvkortetYtelse> = emptyList(),
) : Aarsoppgjoer()

data class Inntektsavkorting(
    val grunnlag: ForventetInntekt,
    val avkortingsperioder: List<Avkortingsperiode> = emptyList(),
    val avkortetYtelseForventetInntekt: List<AvkortetYtelse> = emptyList(),
) {
    init {
        if (!avkortingsperioder.zipWithNext().all { it.first.periode.fom < it.second.periode.fom }) {
            throw InternfeilException("Avkortingsperioder er i feil rekkefølge.")
        }
        if (!avkortetYtelseForventetInntekt.zipWithNext().all { it.first.periode.fom < it.second.periode.fom }) {
            throw InternfeilException("AvkortetYtelseForventetInntekt er i feil rekkefølge.")
        }
    }
}

data class YtelseFoerAvkorting(
    val beregning: Int,
    val periode: Periode,
    val beregningsreferanse: UUID,
)

data class Avkortingsperiode(
    val id: UUID,
    val periode: Periode,
    val avkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
    val inntektsgrunnlag: UUID,
)

data class Restanse(
    val id: UUID,
    val totalRestanse: Int,
    val fordeltRestanse: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode?,
    val kilde: Grunnlagsopplysning.Kilde,
)

data class AvkortetYtelse(
    val id: UUID,
    val type: AvkortetYtelseType,
    val periode: Periode,
    val ytelseEtterAvkorting: Int,
    val ytelseEtterAvkortingFoerRestanse: Int,
    val restanse: Restanse?,
    val avkortingsbeloep: Int,
    val ytelseFoerAvkorting: Int,
    val tidspunkt: Tidspunkt,
    val regelResultat: JsonNode,
    val kilde: Grunnlagsopplysning.RegelKilde,
    val inntektsgrunnlag: UUID? = null,
    val sanksjon: SanksjonertYtelse? = null,
)

enum class AvkortetYtelseType { FORVENTET_INNTEKT, AARSOPPGJOER, ETTEROPPGJOER }

data class MaanedInnvilget(
    val maaned: YearMonth,
    val innvilget: Boolean,
)
