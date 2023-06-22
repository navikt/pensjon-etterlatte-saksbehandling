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
                attestantIdent
            ),
            utbetalingsinfo = finnUtbetalingsinfo(vedtak.behandling.id, vedtak.virkningstidspunkt, brukerTokenInfo),
            avkortingsinfo = finnAvkortingsinfo(
                vedtak.behandling.id,
                vedtak.sak.sakType,
                vedtak.virkningstidspunkt,
                brukerTokenInfo
            ),
            revurderingsaarsak = vedtak.behandling.revurderingsaarsak,
            revurderingInfo = vedtak.behandling.revurderingInfo
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
        val grunnbeloep = beregningsperioder.first().grunnbeloep

        return Utbetalingsinfo(
            antallBarn,
            Kroner(beregningsperioder.hentUtbetaltBeloep()),
            grunnbeloep,
            virkningstidspunkt.atDay(1),
            soeskenjustering,
            beregningsperioder
        )
    }

    private suspend fun finnAvkortingsinfo(
        behandlingId: UUID,
        sakType: SakType,
        virkningstidspunkt: YearMonth,
        brukerTokenInfo: BrukerTokenInfo
    ): Avkortingsinfo? {
        if (sakType == SakType.BARNEPENSJON) return null // TODO: Fjern når avkorting støttes for barnepensjon
        val avkorting = beregningKlient.hentAvkorting(behandlingId, brukerTokenInfo)

        val beregningsperioder = avkorting.avkortetYtelse.map {
            AvkortetBeregningsperiode(
                datoFOM = it.fom,
                datoTOM = it.tom,
                inntekt = Kroner(123456), // TODO: Finn denne fra aarsinntekt - fratrekkInnut
                utbetaltBeloep = Kroner(it.ytelseEtterAvkorting)
            )
        }

        val aarsInntekt = avkorting.avkortingGrunnlag.last().aarsinntekt

        return Avkortingsinfo(
            inntekt = Kroner(aarsInntekt),
            virkningsdato = virkningstidspunkt.atDay(1),
            beregningsperioder
        )
    }
}