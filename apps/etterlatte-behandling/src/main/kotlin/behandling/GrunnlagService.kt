package no.nav.etterlatte.behandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import java.util.UUID

class GrunnlagService(private val grunnlagKlient: GrunnlagKlientImpl) {
    fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
    ) {
        runBlocking {
            val grunnlagsbehov = grunnlagsbehov(behandling, persongalleri)
            grunnlagKlient.leggInnNyttGrunnlag(behandling.id, grunnlagsbehov)
        }
    }

    fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: Long,
        sakType: SakType,
    ) {
        runBlocking {
            grunnlagKlient.oppdaterGrunnlag(
                behandlingId,
                OppdaterGrunnlagRequest(sakId, sakType),
            )
        }
    }

    fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
    ) = runBlocking {
        grunnlagKlient.lagreNyeSaksopplysninger(behandlingId, opplysninger)
    }

    suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri {
        return grunnlagKlient.hentPersongalleri(behandlingId)
            ?.opplysning
            ?: throw NoSuchElementException("Persongalleri mangler for behandling id=$behandlingId")
    }

    private fun grunnlagsbehov(
        behandling: Behandling,
        persongalleri: Persongalleri,
    ): Opplysningsbehov {
        return Opplysningsbehov(
            sakId = behandling.sak.id,
            sakType = behandling.sak.sakType,
            persongalleri = persongalleri,
        )
    }
}
