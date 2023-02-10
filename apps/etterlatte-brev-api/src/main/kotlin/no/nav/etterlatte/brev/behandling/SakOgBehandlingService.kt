package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.brev.vedtak.VedtaksvurderingKlient
import no.nav.etterlatte.libs.common.vedtak.Periode

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

        val innloggetSaksbehandlerEnhet =
            saksbehandlere[innloggetSaksbehandlerIdent] ?: throw SaksbehandlerManglerEnhetException(
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
            utbetalingsinfo = finnUtbetalingsinfo(behandlingId, vedtak.virk, accessToken)
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
                antallBarn = (it.soeskenFlokk?.size ?: 0) + 1,
                utbetaltBeloep = it.utbetaltBeloep,
                trygdetid = it.trygdetid
            )
        }

        val soeskenjustering = beregning.beregningsperioder.any { !it.soeskenFlokk.isNullOrEmpty() }

        return Utbetalingsinfo(
            beregningsperioder.hentUtbetaltBeloep(),
            virkningstidspunkt,
            soeskenjustering,
            beregningsperioder
        )
    }
}

class SaksbehandlerManglerEnhetException(message: String) : Exception(message)