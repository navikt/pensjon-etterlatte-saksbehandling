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
import no.nav.etterlatte.token.Bruker
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.YearMonth
import java.util.*

class SakOgBehandlingService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningKlient: BeregningKlient,
    private val behandlingKlient: BehandlingKlient
) {

    suspend fun hentBehandling(
        sakId: Long,
        behandlingId: UUID,
        bruker: Bruker
    ): Behandling = coroutineScope {
        val vedtak = async { vedtaksvurderingKlient.hentVedtak(behandlingId, bruker) }
        val grunnlag = async { grunnlagKlient.hentGrunnlag(sakId, bruker) }
        val sak = async { behandlingKlient.hentSak(sakId, bruker) }

        mapBehandling(
            vedtak.await(),
            grunnlag.await(),
            sak.await(),
            bruker
        )
    }

    private suspend fun mapBehandling(
        vedtak: VedtakDto,
        grunnlag: Grunnlag,
        sak: Sak,
        bruker: Bruker
    ): Behandling {
        val innloggetSaksbehandlerIdent = bruker.ident()

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
            utbetalingsinfo = finnUtbetalingsinfo(vedtak.behandling.id, vedtak.virkningstidspunkt, bruker),
            avkortingsinfo = finnAvkortingsinfo(
                vedtak.behandling.id,
                vedtak.sak.sakType,
                vedtak.virkningstidspunkt,
                bruker
            ),
            revurderingsaarsak = vedtak.behandling.revurderingsaarsak
        )
    }

    private suspend fun finnUtbetalingsinfo(
        behandlingId: UUID,
        virkningstidspunkt: YearMonth,
        bruker: Bruker
    ): Utbetalingsinfo {
        val beregning = beregningKlient.hentBeregning(behandlingId, bruker)

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
        bruker: Bruker
    ): Avkortingsinfo? {
        if (sakType == SakType.BARNEPENSJON) return null // TODO: Fjern når avkorting støttes for barnepensjon
        val avkorting = beregningKlient.hentAvkorting(behandlingId, bruker)

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