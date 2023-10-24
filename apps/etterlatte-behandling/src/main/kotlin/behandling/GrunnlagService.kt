package no.nav.etterlatte.behandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import java.util.UUID

class GrunnlagService(private val grunnlagKlient: GrunnlagKlientImpl) {
    /**
     * TODO:
     *  Grunnet måten grunnlagsflyten ved førstegangsbehandling er satt opp må det gjøres på denne måten.
     *  Når persongalleriet er fjernet helt fra etterlatte-behandling kan vi begynne arbeidet med å skrive om
     *  måten grunnlag håndterer data på og hvordan dataene oppdateres/behandles.
     */
    fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
    ) {
        runBlocking {
            val grunnlagsbehov = grunnlagsbehov(behandling, persongalleri)
            grunnlagKlient.leggInnNyttGrunnlag(behandling.id, grunnlagsbehov)
        }
    }

    fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
    ) = runBlocking {
        grunnlagKlient.lagreNyeSaksopplysninger(opplysninger.sakId, behandlingId, opplysninger)
    }

    suspend fun hentPersongalleri(
        sakId: Long,
        behandlingId: UUID,
    ): Persongalleri {
        return grunnlagKlient.hentPersongalleri(sakId, behandlingId)
            ?.opplysning
            ?: throw NoSuchElementException("Persongalleri mangler for sak $sakId")
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
