package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient

class SakOgBehandlingService(
    private val vedtaksvurderingKlient: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val beregningKlient: BeregningKlient,
    private val grunnbeloepKlient: GrunnbeloepKlient
) {

    suspend fun hentBehandling(sakId: Long, behandlingId: String, accessToken: String): Behandling {
        val vedtak = vedtaksvurderingKlient.hentVedtak(behandlingId, accessToken)
        val grunnlag = grunnlagKlient.hentGrunnlag(sakId, accessToken)

        return Behandling(
            sakId = sakId,
            behandlingId = behandlingId,
            spraak = grunnlag.mapSpraak(),
            persongalleri = Persongalleri(
                innsender = grunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(),
                avdoed = grunnlag.mapAvdoed()
            ),
            vedtak = ForenkletVedtak(vedtak.vedtakId, vedtak.type, vedtak.vedtakFattet?.ansvarligEnhet!!),
            utbetalingsinfo = finnUtbetalingsinfo(behandlingId, accessToken)
        )
    }

    private suspend fun finnUtbetalingsinfo(behandlingId: String, accessToken: String): Utbetalingsinfo {
        val beregning = beregningKlient.hentBeregning(behandlingId, accessToken)
        val grunnbeloep = grunnbeloepKlient.hentGrunnbeloep()

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
            grunnbeloep.grunnbeloep,
            soeskenjustering,
            beregningsperioder
        )
    }
}