package no.nav.etterlatte.brev.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.token.Bruker
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

        val saksbehandlerEnhet = vedtak.vedtakFattet?.ansvarligEnhet
            ?: throw SaksbehandlerManglerEnhetException("Vedtak mangler ansvarlig enhet vedtakid: ${vedtak.vedtakId}")
        val saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent

        val attestant = vedtak.vedtakFattet?.let {
            Attestant(
                vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent,
                vedtak.attestasjon?.attesterendeEnhet ?: sak.enhet!!
            )
        }

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
                vedtak.type,
                Saksbehandler(
                    saksbehandlerIdent,
                    saksbehandlerEnhet
                ),
                attestant
            ),
            utbetalingsinfo = finnUtbetalingsinfo(vedtak.behandling.id, vedtak.virkningstidspunkt, bruker)
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
                grunnbeloep = it.grunnbelop,
                antallBarn = (it.soeskenFlokk?.size ?: 0) + 1, // Legger til 1 pga at beregning fjerner soeker
                utbetaltBeloep = it.utbetaltBeloep,
                trygdetid = it.trygdetid
            )
        }

        val soeskenjustering = beregning.beregningsperioder.any { !it.soeskenFlokk.isNullOrEmpty() }
        val antallBarn = if (soeskenjustering) beregningsperioder.last().antallBarn else null

        return Utbetalingsinfo(
            antallBarn,
            beregningsperioder.hentUtbetaltBeloep(),
            virkningstidspunkt.atDay(1),
            soeskenjustering,
            beregningsperioder
        )
    }
}

class SaksbehandlerManglerEnhetException(message: String) : Exception(message)