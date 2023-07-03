package no.nav.etterlatte.brev.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.YearMonth
import java.util.*

class SakOgBehandlingService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningKlient: BeregningKlient,
    private val behandlingKlient: BehandlingKlient
) {

    suspend fun hentSak(sakId: Long, bruker: BrukerTokenInfo) =
        behandlingKlient.hentSak(sakId, bruker)

    suspend fun hentSoeker(sakId: Long, bruker: BrukerTokenInfo): Soeker =
        grunnlagKlient.hentGrunnlag(sakId, bruker)
            .mapSoeker()

    suspend fun hentBehandling(
        sakId: Long,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo
    ): Behandling = coroutineScope {
        val vedtak = async { vedtaksvurderingKlient.hentVedtak(behandlingId, brukerTokenInfo) }
        val grunnlag = async { grunnlagKlient.hentGrunnlag(sakId, brukerTokenInfo) }
        val sak = async { behandlingKlient.hentSak(sakId, brukerTokenInfo) }

        mapBehandling(
            vedtak.await(),
            grunnlag.await(),
            sak.await(),
            brukerTokenInfo
        )
    }

    private suspend fun mapBehandling(
        vedtak: VedtakDto,
        grunnlag: Grunnlag,
        sak: Sak,
        brukerTokenInfo: BrukerTokenInfo
    ): Behandling {
        val innloggetSaksbehandlerIdent = brukerTokenInfo.ident()

        val ansvarligEnhet = vedtak.vedtakFattet?.ansvarligEnhet ?: sak.enhet
        val saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent
        val attestantIdent = vedtak.vedtakFattet?.let { vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent }

        return Behandling(
            sakId = vedtak.sak.id,
            sakType = vedtak.sak.sakType,
            behandlingId = vedtak.behandling.id,
            spraak = grunnlag.mapSpraak(),
            persongalleri = Persongalleri(
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(),
                avdoed = grunnlag.mapAvdoed()
            ),
            vedtak = ForenkletVedtak(
                vedtak.vedtakId,
                vedtak.status,
                vedtak.type,
                ansvarligEnhet,
                saksbehandlerIdent,
                attestantIdent,
                vedtak.vedtakFattet?.tidspunkt?.toLocalDate()
            ),
            utbetalingsinfo = finnUtbetalingsinfo(vedtak.behandling.id, vedtak.virkningstidspunkt, brukerTokenInfo),
            avkortingsinfo = finnYtelseMedGrunnlag(
                vedtak.behandling.id,
                vedtak.sak.sakType,
                vedtak.virkningstidspunkt,
                vedtak.type,
                brukerTokenInfo
            ),
            revurderingsaarsak = vedtak.behandling.revurderingsaarsak,
            revurderingInfo = vedtak.behandling.revurderingInfo,
            virkningsdato = vedtak.virkningstidspunkt
        )
    }

    private suspend fun finnUtbetalingsinfo(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo
    ): Utbetalingsinfo {
        val beregning = beregningKlient.hentBeregning(behandlingId, brukerTokenInfo)

        val beregningsperioder = beregning.beregningsperioder.map {
            Beregningsperiode(
                datoFOM = it.datoFOM.atDay(1),
                datoTOM = it.datoTOM?.atEndOfMonth(),
                grunnbeloep = Kroner(it.grunnbelop),
                antallBarn = (it.soeskenFlokk?.size ?: 0) + 1, // Legger til 1 pga at beregning fjerner soeker
                utbetaltBeloep = Kroner(it.utbetaltBeloep),
                trygdetid = it.trygdetid
            )
        }

        val soeskenjustering = beregning.beregningsperioder.any { !it.soeskenFlokk.isNullOrEmpty() }
        val antallBarn = if (soeskenjustering) beregningsperioder.last().antallBarn else 1

        return Utbetalingsinfo(
            antallBarn,
            Kroner(beregningsperioder.hentUtbetaltBeloep()),
            virkningstidspunkt.atDay(1),
            soeskenjustering,
            beregningsperioder
        )
    }

    private suspend fun finnYtelseMedGrunnlag(
        behandlingId: UUID,
        sakType: SakType,
        virkningstidspunkt: YearMonth,
        vedtakType: VedtakType,
        brukerTokenInfo: BrukerTokenInfo
    ): Avkortingsinfo? {
        // TODO: Fjern sjekken når avkorting støttes for barnepensjon
        if (sakType == SakType.BARNEPENSJON || vedtakType == VedtakType.OPPHOER) return null

        val ytelseMedGrunnlag = beregningKlient.hentYtelseMedGrunnlag(behandlingId, brukerTokenInfo)

        val beregningsperioder = ytelseMedGrunnlag.perioder.map {
            AvkortetBeregningsperiode(
                datoFOM = it.periode.fom.atDay(1),
                datoTOM = it.periode.tom?.atEndOfMonth(),
                inntekt = Kroner(it.aarsinntekt - it.fratrekkInnAar),
                utbetaltBeloep = Kroner(it.ytelseEtterAvkorting)
            )
        }

        val aarsInntekt = ytelseMedGrunnlag.perioder.first().aarsinntekt
        val grunnbeloep = ytelseMedGrunnlag.perioder.first().grunnbelop

        return Avkortingsinfo(
            grunnbeloep = Kroner(grunnbeloep),
            inntekt = Kroner(aarsInntekt),
            virkningsdato = virkningstidspunkt.atDay(1),
            beregningsperioder
        )
    }
}