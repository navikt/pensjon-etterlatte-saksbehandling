package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.YearMonth
import java.util.*
import no.nav.etterlatte.libs.common.beregning.BeregningDTO as CommonBeregningDTO
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode as CommonBeregningsperiode
import no.nav.etterlatte.libs.common.beregning.Beregningstype as CommonBeregningstype

enum class Beregningstype {
    BP,
    OMS;

    companion object {

        fun fraDtoType(dto: CommonBeregningstype) = when (dto) {
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
    val beregningsperioder: List<Beregningsperiode>
) {
    companion object {

        fun fraBeregningDTO(dto: CommonBeregningDTO) = Beregning(
            beregningId = dto.beregningId,
            behandlingId = dto.behandlingId,
            type = Beregningstype.fraDtoType(dto.type),
            beregnetDato = dto.beregnetDato,
            beregningsperioder = dto.beregningsperioder.map(Beregningsperiode::fraBeregningsperiodeDTO)
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
    val trygdetid: Int
) {
    companion object {

        fun fraBeregningsperiodeDTO(dto: CommonBeregningsperiode) = Beregningsperiode(
            datoFOM = dto.datoFOM,
            datoTOM = dto.datoTOM,
            utbetaltBeloep = dto.utbetaltBeloep,
            soeskenFlokk = dto.soeskenFlokk,
            grunnbelopMnd = dto.grunnbelopMnd,
            grunnbelop = dto.grunnbelop,
            trygdetid = dto.trygdetid
        )
    }
}