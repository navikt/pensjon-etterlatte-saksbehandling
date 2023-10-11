package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class GenerellBrevData(
    val sak: Sak,
    val personerISak: PersonerISak,
    val behandlingId: UUID,
    val forenkletVedtak: ForenkletVedtak,
    val spraak: Spraak,
    val revurderingsaarsak: RevurderingAarsak? = null,
)

data class Behandling(
    val sakId: Long,
    val sakType: SakType,
    val behandlingId: UUID,
    val spraak: Spraak,
    val personerISak: PersonerISak,
    val vedtak: ForenkletVedtak,
    val utbetalingsinfo: Utbetalingsinfo? = null,
    val vilkaarsvurdering: VilkaarsvurderingDto,
    val forrigeUtbetalingsinfo: Utbetalingsinfo? = null,
    val avkortingsinfo: Avkortingsinfo? = null,
    val revurderingsaarsak: RevurderingAarsak? = null,
    val revurderingInfo: RevurderingInfo? = null,
    val virkningsdato: YearMonth? = null,
    val opprinneligInnvilgelsesdato: LocalDate? = null, // Kun opphør RevurderingAarsak.OMGJOERING_AV_FARSKAP TODO: fix
    val adopsjonsdato: LocalDate? = null,
    val trygdetid: List<Trygdetidsperiode>? = null,
    val etterbetalingDTO: EtterbetalingDTO?,
) {
    init {
        if (vedtak.type == VedtakType.INNVILGELSE) {
            requireNotNull(utbetalingsinfo) { "Utbetalingsinformasjon mangler på behandling (id=$behandlingId" }
        }
    }
}

data class Trygdetidsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val land: String,
    val opptjeningsperiode: String,
)

data class ForenkletVedtak(
    val id: Long,
    val status: VedtakStatus,
    val type: VedtakType,
    val ansvarligEnhet: String,
    val saksbehandlerIdent: String,
    val attestantIdent: String?,
    val vedtaksdato: LocalDate?,
    val virkningstidspunkt: YearMonth,
    val revurderingInfo: RevurderingInfo? = null,
)

data class Utbetalingsinfo(
    val antallBarn: Int,
    val beloep: Kroner,
    val virkningsdato: LocalDate,
    val soeskenjustering: Boolean,
    val beregningsperioder: List<Beregningsperiode>,
)

data class Avkortingsinfo(
    val grunnbeloep: Kroner,
    val inntekt: Kroner,
    val virkningsdato: LocalDate,
    val beregningsperioder: List<AvkortetBeregningsperiode>,
)

data class AvkortetBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val inntekt: Kroner,
    val ytelseFoerAvkorting: Kroner,
    val trygdetid: Int,
    val utbetaltBeloep: Kroner,
)

data class Beregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val antallBarn: Int,
    val utbetaltBeloep: Kroner,
    val trygdetid: Int,
    val institusjon: Boolean,
)

fun List<Beregningsperiode>.hentUtbetaltBeloep(): Int {
    // TODO: Håndter grunnbeløpsendringer
    return this.last().utbetaltBeloep.value
}
