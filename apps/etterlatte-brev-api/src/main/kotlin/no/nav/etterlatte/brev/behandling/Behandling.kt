package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.model.EtterbetalingDTO
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.trygdetid.BeregnetTrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
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
    val systemkilde: Vedtaksloesning,
    val utlandstilknytning: Utlandstilknytning? = null,
    val revurderingsaarsak: Revurderingaarsak? = null,
)

data class Trygdetid(
    val aarTrygdetid: Int,
    val prorataBroek: IntBroek?,
    val maanederTrygdetid: Int,
    val perioder: List<Trygdetidsperiode>,
    val overstyrt: Boolean,
)

data class Trygdetidsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val land: String,
    val opptjeningsperiode: BeregnetTrygdetidGrunnlagDto?,
)

data class ForenkletVedtak(
    val id: Long,
    val status: VedtakStatus,
    val type: VedtakType,
    val ansvarligEnhet: String,
    val saksbehandlerIdent: String,
    val attestantIdent: String?,
    val vedtaksdato: LocalDate?,
    val virkningstidspunkt: YearMonth? = null,
    val revurderingInfo: RevurderingInfo? = null,
    val tilbakekreving: Tilbakekreving? = null,
)

data class Utbetalingsinfo(
    val antallBarn: Int,
    val beloep: Kroner,
    val virkningsdato: LocalDate,
    val soeskenjustering: Boolean,
    val beregningsperioder: List<Beregningsperiode>,
) {
    companion object {
        fun kopier(
            utbetalingsinfo: Utbetalingsinfo,
            etterbetalingDTO: EtterbetalingDTO?,
        ) = if (etterbetalingDTO == null) {
            utbetalingsinfo
        } else {
            utbetalingsinfo.copy(
                beregningsperioder =
                    utbetalingsinfo.beregningsperioder.filter {
                        YearMonth.from(it.datoFOM) > YearMonth.from(etterbetalingDTO.datoTom)
                    },
            )
        }
    }
}

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
    val prorataBroek: IntBroek?,
    val institusjon: Boolean,
)

fun List<Beregningsperiode>.hentUtbetaltBeloep(): Int {
    // TODO: Håndter grunnbeløpsendringer
    return this.last().utbetaltBeloep.value
}
