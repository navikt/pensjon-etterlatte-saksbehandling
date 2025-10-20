package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.beregning.grunnlag.InstitusjonsoppholdBeregningsgrunnlag
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.AvkortetYtelseDto
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.ForventetInntektDto
import no.nav.etterlatte.libs.common.beregning.SanksjonertYtelse
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import no.nav.etterlatte.libs.common.beregning.BeregningDTO as CommonBeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode as CommonBeregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype as CommonBeregningstype

enum class Beregningstype {
    BP,
    OMS,
    ;

    companion object {
        fun fraDtoType(dto: CommonBeregningstype) =
            when (dto) {
                CommonBeregningstype.BP -> BP
                CommonBeregningstype.OMS -> OMS
            }
    }
}

data class Beregning(
    val beregningId: UUID,
    val behandlingId: UUID,
    val type: Beregningstype,
    val beregnetDato: Tidspunkt,
    val beregningsperioder: List<Beregningsperiode>,
    // TODO: Sett denne som non-nullable etter at vi har refreshet hentede beregninger
    val overstyrtBeregning: Boolean? = null,
) {
    companion object {
        fun fraBeregningDTO(dto: CommonBeregningDTO) =
            Beregning(
                beregningId = dto.beregningId,
                behandlingId = dto.behandlingId,
                type = Beregningstype.fraDtoType(dto.type),
                beregnetDato = dto.beregnetDato,
                beregningsperioder = dto.beregningsperioder.map(Beregningsperiode::fraBeregningsperiodeDTO),
                overstyrtBeregning = dto.overstyrBeregning != null,
            )
    }
}

data class InstitusjonsoppholdStatistikk(
    val sats: BigDecimal,
) {
    companion object {
        fun fra(dto: InstitusjonsoppholdBeregningsgrunnlag): InstitusjonsoppholdStatistikk =
            InstitusjonsoppholdStatistikk(
                sats = dto.prosentEtterReduksjon().verdi.toBigDecimal() / 100.toBigDecimal(),
            )
    }
}

data class Beregningsperiode(
    val datoFOM: YearMonth,
    val datoTOM: YearMonth?,
    val utbetaltBeloep: Int,
    val soeskenFlokk: List<String>?,
    val grunnbelopMnd: Int,
    val grunnbelop: Int,
    val trygdetid: Int,
    val institusjonsopphold: InstitusjonsoppholdStatistikk? = null,
    val beregningsMetode: BeregningsMetode? = null,
    val samletNorskTrygdetid: Int? = null,
    val samletTeoretiskTrygdetid: Int? = null,
    val broek: IntBroek? = null,
    val anvendteAvdoede: List<String?>? = null,
) {
    fun anvendtSats(
        beregningstype: Beregningstype,
        erForeldreloes: Boolean,
        erOverstyrt: Boolean,
    ): String {
        if (institusjonsopphold != null) {
            return "${institusjonsopphold.sats.toPlainString()} G"
        }
        if (erOverstyrt) {
            return "MANUELT_OVERSTYRT"
        }
        if (beregningstype == Beregningstype.OMS) {
            return "2.25 G"
        }
        if (datoFOM < YearMonth.of(2024, Month.JANUARY)) {
            return "SOESKENJUSTERING"
        }
        val foreldreloesIPeriode = anvendteAvdoede?.size?.let { it == 2 } ?: erForeldreloes

        return when (foreldreloesIPeriode) {
            true -> "2.25 G"
            false -> "1 G"
        }
    }

    companion object {
        fun fraBeregningsperiodeDTO(dto: CommonBeregningsperiode) =
            Beregningsperiode(
                datoFOM = dto.datoFOM,
                datoTOM = dto.datoTOM,
                utbetaltBeloep = dto.utbetaltBeloep,
                soeskenFlokk = dto.soeskenFlokk,
                grunnbelopMnd = dto.grunnbelopMnd,
                grunnbelop = dto.grunnbelop,
                trygdetid = dto.trygdetid,
                institusjonsopphold = dto.institusjonsopphold?.let { InstitusjonsoppholdStatistikk.fra(it) },
                beregningsMetode = dto.beregningsMetode,
                samletNorskTrygdetid = dto.samletNorskTrygdetid,
                samletTeoretiskTrygdetid = dto.samletTeoretiskTrygdetid,
                broek = dto.broek,
                anvendteAvdoede = dto.avdoedeForeldre,
            )
    }
}

data class Avkorting(
    val avkortingGrunnlag: List<AvkortingGrunnlag>,
    val avkortetYtelse: List<AvkortetYtelse>,
) {
    companion object {
        fun fraDTO(dto: AvkortingDto) =
            Avkorting(
                // Grunnlaget for avkortingen for etteroppgjør er fanget opp i etteroppgjørstatistikk
                avkortingGrunnlag =
                    dto.avkortingGrunnlag.filterIsInstance<ForventetInntektDto>().map {
                        AvkortingGrunnlag.fraDTO(it)
                    },
                avkortetYtelse = dto.avkortetYtelse.map { AvkortetYtelse.fraDTO(it) },
            )
    }
}

data class AvkortingGrunnlag(
    val fom: YearMonth,
    val tom: YearMonth?,
    val aarsinntekt: Int,
    val fratrekkInnAar: Int,
    val relevanteMaanederInnAar: Int?,
    val spesifikasjon: String,
) {
    companion object {
        fun fraDTO(dto: ForventetInntektDto) =
            AvkortingGrunnlag(
                fom = dto.fom,
                tom = dto.tom,
                aarsinntekt = dto.inntektTom,
                fratrekkInnAar = dto.fratrekkInnAar,
                relevanteMaanederInnAar = dto.innvilgaMaaneder,
                spesifikasjon = dto.spesifikasjon,
            )
    }
}

data class AvkortetYtelse(
    val fom: YearMonth,
    val tom: YearMonth?,
    val ytelseFoerAvkorting: Int,
    val avkortingsbeloep: Int,
    val ytelseEtterAvkorting: Int,
    val restanse: Int,
    val sanksjonertYtelse: SanksjonertYtelse?,
) {
    companion object {
        fun fraDTO(dto: AvkortetYtelseDto) =
            AvkortetYtelse(
                fom = dto.fom,
                tom = dto.tom,
                ytelseFoerAvkorting = dto.ytelseFoerAvkorting,
                avkortingsbeloep = dto.avkortingsbeloep,
                ytelseEtterAvkorting = dto.ytelseEtterAvkorting,
                restanse = dto.restanse,
                sanksjonertYtelse = dto.sanksjon,
            )
    }
}
