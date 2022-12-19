package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.beregning.BeregningKlient
import no.nav.etterlatte.brev.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import java.time.LocalDate

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
            vedtak = ForenkletVedtak(vedtak.vedtakId, vedtak.type),
            utbetalingsinfo = finnUtbetalingsinfo(behandlingId, accessToken)
        )
    }

    private suspend fun finnUtbetalingsinfo(behandlingId: String, accessToken: String): Utbetalingsinfo {
        val beregning = beregningKlient.hentBeregning(behandlingId, accessToken)
        val grunnbeloep = grunnbeloepKlient.hentGrunnbeloep()

        // TODO: Tilføye beregningsperioder i brevet. Se https://jira.adeo.no/browse/EY-1403
        val periode = beregning.beregningsperioder.first()

        return Utbetalingsinfo(
            beloep = periode.utbetaltBeloep,
            virkningsdato = LocalDate.of(periode.datoFOM.year, periode.datoFOM.month, 1),
            kontonummer = "<todo: Ikke tilgjengelig>",
            grunnbeloep = grunnbeloep,
            antallBarn = (periode.soeskenFlokk?.size ?: 0) + 1, // TODO: Må fikse dette ifm TODO på linje 42
            soeskenjustering = !periode.soeskenFlokk.isNullOrEmpty()
        )
    }
}
