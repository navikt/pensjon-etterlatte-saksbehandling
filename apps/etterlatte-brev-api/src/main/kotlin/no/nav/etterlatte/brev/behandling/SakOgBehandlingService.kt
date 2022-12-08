package no.nav.etterlatte.brev.behandling

import no.nav.etterlatte.brev.grunnbeloep.Grunnbeloep
import no.nav.etterlatte.brev.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.brev.grunnlag.GrunnlagKlient
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import java.time.LocalDate

class SakOgBehandlingService(
    private val vedtakService: VedtaksvurderingKlient,
    private val grunnlagKlient: GrunnlagKlient,
    private val grunnbeloepKlient: GrunnbeloepKlient
) {

    suspend fun hentBehandling(sakId: Long, behandlingId: String, accessToken: String): Behandling {
        val vedtak = vedtakService.hentVedtak(behandlingId, accessToken)
        val grunnlag = grunnlagKlient.hentGrunnlag(sakId, accessToken)
        val innsenderGrunnlag = grunnlagKlient.hentGrunnlag(sakId, Opplysningstype.INNSENDER_SOEKNAD_V1, accessToken)
        val grunnbeloep = grunnbeloepKlient.hentGrunnbeloep()

        return Behandling(
            sakId,
            behandlingId,
            persongalleri = Persongalleri(
                innsender = innsenderGrunnlag.mapInnsender(),
                soeker = grunnlag.mapSoeker(),
                avdoed = grunnlag.mapAvdoed()
            ),
            // TODO: Soesken må inn
            vedtak,
            grunnlag,
            utbetalingsinfo = finnUtbetalingsinfo(vedtak, grunnbeloep)
        )
    }

    private fun finnUtbetalingsinfo(
        vedtak: Vedtak,
        grunnbeloep: Grunnbeloep
    ) = Utbetalingsinfo(
        beloep = vedtak.beregning!!.sammendrag[0].beloep,
        virkningsdato = LocalDate.of(vedtak.virk.fom.year, vedtak.virk.fom.month, 1),
        kontonummer = "<todo: Ikke tilgjengelig>",
        grunnbeloep = grunnbeloep
    )
}
