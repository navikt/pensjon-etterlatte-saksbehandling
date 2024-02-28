package no.nav.etterlatte.behandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.sak.Sak
import java.util.UUID

interface GrunnlagService {
    fun leggInnNyttGrunnlagSak(
        sak: Sak,
        persongalleri: Persongalleri,
    )

    fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
    )

    fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: Long,
        sakType: SakType,
    )

    fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
    )

    suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri
}

class GrunnlagServiceImpl(private val grunnlagKlient: GrunnlagKlientImpl) : GrunnlagService {
    override fun leggInnNyttGrunnlagSak(
        sak: Sak,
        persongalleri: Persongalleri,
    ) {
        runBlocking {
            val grunnlagsbehov = grunnlagsbehovSak(sak, persongalleri)
            grunnlagKlient.leggInnNyttGrunnlagSak(sak.id, grunnlagsbehov)
        }
    }

    override fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
    ) {
        runBlocking {
            val grunnlagsbehov = grunnlagsbehov(behandling, persongalleri)
            grunnlagKlient.leggInnNyttGrunnlag(behandling.id, grunnlagsbehov)
        }
    }

    override fun oppdaterGrunnlag(
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

    override fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
    ) = runBlocking {
        grunnlagKlient.lagreNyeSaksopplysninger(behandlingId, opplysninger)
    }

    override suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri {
        return grunnlagKlient.hentPersongalleri(behandlingId)
            ?.opplysning
            ?: throw NoSuchElementException("Persongalleri mangler for behandling id=$behandlingId")
    }

    private fun grunnlagsbehovSak(
        sak: Sak,
        persongalleri: Persongalleri,
    ): Opplysningsbehov {
        return Opplysningsbehov(
            sakId = sak.id,
            sakType = sak.sakType,
            persongalleri = persongalleri,
        )
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
