package no.nav.etterlatte.behandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov

class GrunnlagService(private val grunnlagKlient: GrunnlagKlientImpl) {
    /**
     * TODO:
     *  Grunnet måten grunnlagsflyten ved førstegangsbehandling er satt opp må det gjøres på denne måten.
     *  Når persongalleriet er fjernet helt fra etterlatte-behandling kan vi begynne arbeidet med å skrive om
     *  måten grunnlag håndterer data på og hvordan dataene oppdateres/behandles.
     */
    fun leggInnNyttGrunnlag(behandling: Behandling, persongalleri: Persongalleri) {
        runBlocking {
            val grunnlagsbehov = grunnlagsbehov(behandling, persongalleri)
            grunnlagKlient.leggInnNyttGrunnlag(grunnlagsbehov)
        }
    }

    fun leggTilNyeOpplysninger(sakId: Long, opplysninger: NyeSaksopplysninger) = runBlocking {
        grunnlagKlient.lagreNyeSaksopplysninger(sakId, opplysninger)
    }

    suspend fun hentPersongalleri(sakId: Long): Persongalleri {
        return grunnlagKlient.hentPersongalleri(sakId)
            ?.opplysning
            ?: throw NoSuchElementException("Persongalleri mangler for sak $sakId")
    }

    private fun grunnlagsbehov(behandling: Behandling, persongalleri: Persongalleri): Opplysningsbehov {
        return Opplysningsbehov(
            sakid = behandling.sak.id,
            sakType = behandling.sak.sakType,
            persongalleri = persongalleri
        )
    }
}