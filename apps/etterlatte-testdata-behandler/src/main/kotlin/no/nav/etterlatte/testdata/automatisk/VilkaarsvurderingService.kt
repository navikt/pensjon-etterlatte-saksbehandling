package no.nav.etterlatte.testdata.automatisk

import com.github.michaelbull.result.mapBoth
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.DownstreamResourceClient
import no.nav.etterlatte.libs.ktor.ktor.ktorobo.Resource
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.vilkaarsvurdering.VurdertVilkaarsvurderingResultatDto
import no.nav.etterlatte.testdata.BEGRUNNELSE
import org.slf4j.LoggerFactory
import java.util.UUID

class VilkaarsvurderingService(
    private val klient: DownstreamResourceClient,
    private val url: String,
    private val clientId: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun vilkaarsvurder(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) {
        logger.info("Oppretter vilkårsvurdering for gjenoppretting for $behandlingId")
        retryOgPakkUt { opprettVilkaarsvurdering(behandlingId, bruker) }

        logger.info("Oppdaterer vilkårene med korrekt utfall for gjenoppretting $behandlingId")

        retryOgPakkUt { settVilkaarsvurderingaSomHelhetSomOppfylt(behandlingId, bruker) }
    }

    private suspend fun opprettVilkaarsvurdering(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = klient
        .post(
            Resource(
                clientId = clientId,
                url = "$url/api/vilkaarsvurdering/$behandlingId/opprett",
            ),
            bruker,
            {},
        ).mapBoth(
            success = {},
            failure = { throw it },
        )

    private suspend fun settVilkaarsvurderingaSomHelhetSomOppfylt(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = klient
        .post(
            Resource(
                clientId = clientId,
                url = "$url/api/vilkaarsvurdering/resultat/$behandlingId",
            ),
            bruker,
            VurdertVilkaarsvurderingResultatDto(
                resultat = VilkaarsvurderingUtfall.OPPFYLT,
                kommentar = BEGRUNNELSE,
            ),
        ).mapBoth(
            success = {},
            failure = { throw it },
        )
}
