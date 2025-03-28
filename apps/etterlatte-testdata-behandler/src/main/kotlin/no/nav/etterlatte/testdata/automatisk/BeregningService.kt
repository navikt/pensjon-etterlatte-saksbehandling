package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.beregning.grunnlag.LagreBeregningsGrunnlag
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetodeBeregningsgrunnlag
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.testdata.BEGRUNNELSE
import java.util.UUID

class BeregningService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    suspend fun beregn(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = retryOgPakkUt {
        klient.post(Resource(clientId, "$url/api/beregning/$behandlingId"), bruker, {}).mapBoth(
            success = {},
            failure = { throw it },
        )
    }

    suspend fun lagreBeregningsgrunnlag(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = retryOgPakkUt {
        klient
            .post(
                Resource(clientId, "$url/api/beregning/beregningsgrunnlag/$behandlingId"),
                bruker,
                LagreBeregningsGrunnlag(
                    soeskenMedIBeregning = emptyList(),
                    institusjonsopphold = emptyList(),
                    beregningsMetode =
                        BeregningsMetodeBeregningsgrunnlag(
                            beregningsMetode = BeregningsMetode.NASJONAL,
                            begrunnelse = BEGRUNNELSE,
                        ),
                ),
            ).mapBoth(
                success = {},
                failure = { throw it },
            )
    }
}
