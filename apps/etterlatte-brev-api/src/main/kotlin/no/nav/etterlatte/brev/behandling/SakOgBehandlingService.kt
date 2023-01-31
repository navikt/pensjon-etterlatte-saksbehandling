package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient

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
    ): Behandling {
        val vedtak = vedtaksvurderingKlient.hentVedtak(behandlingId, accessToken)
        val grunnlag = grunnlagKlient.hentGrunnlag(sakId, accessToken)

        val innloggetSaksbehandlerEnhet = saksbehandlere[innloggetSaksbehandlerIdent]
            ?: throw SaksbehandlerManglerEnhetException(
                "Saksbehandler $innloggetSaksbehandlerIdent mangler enhet fra secret"
            )

        val saksbehandlerEnhet = vedtak.vedtakFattet?.ansvarligEnhet ?: innloggetSaksbehandlerEnhet
        val saksbehandlerIdent = vedtak.vedtakFattet?.ansvarligSaksbehandler ?: innloggetSaksbehandlerIdent
        val attestant = if (vedtak.vedtakFattet != null) (
            Attestant(
                vedtak.attestasjon?.attestant
                    ?: innloggetSaksbehandlerIdent,
                vedtak.attestasjon?.attesterendeEnhet
                    ?: innloggetSaksbehandlerEnhet
            )
            ) else null

        return Behandling(
            sakId = sakId,
            behandlingId = behandlingId,
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

            utbetalingsinfo = finnUtbetalingsinfo(behandlingId, accessToken)
        )
    }

    private suspend fun finnUtbetalingsinfo(behandlingId: String, accessToken: String): Utbetalingsinfo {
        val beregning = beregningKlient.hentBeregning(behandlingId, accessToken)

        val beregningsperioder = beregning.beregningsperioder.map {
            Beregningsperiode(
                datoFOM = it.datoFOM.atDay(1),
                datoTOM = it.datoTOM?.atEndOfMonth(),
                grunnbeloep = it.grunnbelopMnd,
                antallBarn = (it.soeskenFlokk?.size ?: 0) + 1,
                utbetaltBeloep = it.utbetaltBeloep,
                trygdetid = it.trygdetid
            )
        }

        val soeskenjustering = beregning.beregningsperioder
            .any { !it.soeskenFlokk.isNullOrEmpty() }

        return Utbetalingsinfo(
            soeskenjustering,
            beregningsperioder
        )
    }
}

class SaksbehandlerManglerEnhetException(message: String) : Exception(message)