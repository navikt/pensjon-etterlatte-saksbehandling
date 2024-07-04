package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.adresse.AvsenderRequest
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tilbakekreving.Tilbakekreving
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

fun avsenderRequest(
    bruker: BrukerTokenInfo,
    forenkletVedtak: ForenkletVedtak?,
    enhet: String,
) = forenkletVedtak?.let {
    AvsenderRequest(
        saksbehandlerIdent = it.saksbehandlerIdent,
        sakenhet = it.sakenhet,
        attestantIdent = it.attestantIdent,
    )
} ?: AvsenderRequest(saksbehandlerIdent = bruker.ident(), sakenhet = enhet)

data class GenerellBrevData(
    val sak: Sak,
    val personerISak: PersonerISak,
    val behandlingId: UUID?,
    val forenkletVedtak: ForenkletVedtak?,
    val spraak: Spraak,
    val systemkilde: Vedtaksloesning,
    val utlandstilknytning: Utlandstilknytning? = null,
    val revurderingsaarsak: Revurderingaarsak? = null,
) {
    fun avsenderRequest(bruker: BrukerTokenInfo) = avsenderRequest(bruker, forenkletVedtak, sak.enhet)

    // TODO På tide å fjerne?
    // Tidligere erMigrering - Vil si saker som er løpende i Pesys når det vedtas i Gjenny og opphøres etter vedtaket.
    fun loependeIPesys() = systemkilde == Vedtaksloesning.PESYS && behandlingId != null && revurderingsaarsak == null

    fun vedtakstype() =
        forenkletVedtak
            ?.type
            ?.name
            ?.lowercase()
            ?.replace("_", " ")

    fun erForeldreloes() =
        with(personerISak) {
            // TODO soeker.foreldreloes benyttes nå kun hvis valgt ved manuell behandling. Må også brukes ved ukjent forelder etter søknad
            soeker.foreldreloes ||
                avdoede.size > 1
        }
}

data class ForenkletVedtak(
    val id: Long,
    val status: VedtakStatus,
    val type: VedtakType,
    val sakenhet: String,
    val saksbehandlerIdent: String,
    val attestantIdent: String?,
    val vedtaksdato: LocalDate?,
    val virkningstidspunkt: YearMonth? = null,
    val revurderingInfo: RevurderingInfo? = null,
    val tilbakekreving: Tilbakekreving? = null,
    val klage: Klage? = null,
)

data class Utbetalingsinfo(
    val antallBarn: Int,
    val beloep: Kroner,
    val virkningsdato: LocalDate,
    val soeskenjustering: Boolean,
    val beregningsperioder: List<Beregningsperiode>,
)

data class Avkortingsinfo(
    val virkningsdato: LocalDate,
    val beregningsperioder: List<AvkortetBeregningsperiode>,
)

data class AvkortetBeregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val inntekt: Kroner,
    val aarsinntekt: Kroner,
    val fratrekkInnAar: Kroner,
    val relevanteMaanederInnAar: Int,
    val ytelseFoerAvkorting: Kroner,
    val restanse: Kroner,
    val trygdetid: Int,
    val utbetaltBeloep: Kroner,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
)

data class Beregningsperiode(
    val datoFOM: LocalDate,
    val datoTOM: LocalDate?,
    val grunnbeloep: Kroner,
    val antallBarn: Int,
    val utbetaltBeloep: Kroner,
    val trygdetid: Int,
    val trygdetidForIdent: String? = null,
    val prorataBroek: IntBroek?,
    val institusjon: Boolean,
    val beregningsMetodeAnvendt: BeregningsMetode,
    val beregningsMetodeFraGrunnlag: BeregningsMetode,
)

fun List<Beregningsperiode>.hentUtbetaltBeloep(): Int {
    // TODO: Håndter grunnbeløpsendringer
    return this.last().utbetaltBeloep.value
}
