package no.nav.etterlatte.brev.behandling

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Vedtak

class SakOgBehandlingService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningKlient: BeregningKlient,
    private val saksbehandlere: Map<String, String>
) {

    suspend fun hentBehandling(
        sakId: Long,
        behandlingId: String,
        innloggetSaksbehandlerIdent: String,
        accessToken: String
    ): Behandling = coroutineScope {
        val vedtak = async { vedtaksvurderingKlient.hentVedtak(behandlingId, accessToken) }
        val grunnlag = async { grunnlagKlient.hentGrunnlag(sakId, accessToken) }

        mapBehandling(
            vedtak.await(),
            grunnlag.await(),
            innloggetSaksbehandlerIdent,
            accessToken
        )
    }

    private suspend fun mapBehandling(
        vedtak: Vedtak,
        grunnlag: Grunnlag,
        innloggetSaksbehandlerIdent: String,
        accessToken: String
    ): Behandling {
        val innloggetSaksbehandlerEnhet = saksbehandlere[innloggetSaksbehandlerIdent]
            ?: throw SaksbehandlerManglerEnhetException(
                "Saksbehandler $innloggetSaksbehandlerIdent mangler enhet fra secret"
            )

        val saksbehandlerEnhet = vedtak.vedtakFattet?.ansvarligEnhet ?: innloggetSaksbehandlerEnhet
        val saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent
        val attestant = vedtak.vedtakFattet?.let {
            Attestant(
                vedtak.attestasjon?.attestant ?: innloggetSaksbehandlerIdent,
                vedtak.attestasjon?.attesterendeEnhet ?: innloggetSaksbehandlerEnhet
            )
        }

        return Behandling(
            sakId = vedtak.sak.id,
            behandlingId = vedtak.behandling.id.toString(),
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
            utbetalingsinfo = finnUtbetalingsinfo(vedtak.behandling.id.toString(), vedtak.virk, accessToken)
        )
    }

    private suspend fun finnUtbetalingsinfo(behandlingId: String, virk: Periode, accessToken: String): Utbetalingsinfo {
        val beregning = beregningKlient.hentBeregning(behandlingId, accessToken)

        val virkningstidspunkt = virk.fom.atDay(1)

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
            virkningstidspunkt,
            soeskenjustering,
            beregningsperioder
        )
    }
}

class SaksbehandlerManglerEnhetException(message: String) : Exception(message)