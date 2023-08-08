package no.nav.etterlatte.behandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.opplysningsbehov.Opplysningsbehov

class GrunnlagService(private val grunnlagKlient: GrunnlagKlientImpl) {
    fun leggInnNyttGrunnlag(behandling: Behandling) {
        runBlocking {
            val grunnlagsbehov = grunnlagsbehov(behandling)
            grunnlagKlient.leggInnNyttGrunnlag(grunnlagsbehov)
        }
    }

    private suspend fun grunnlagsbehov(behandling: Behandling): Opplysningsbehov {
        val sakId = behandling.sak.id
        val persongalleri: Persongalleri = grunnlagKlient.hentPersongalleri(sakId)
            ?.opplysning
            ?: throw NoSuchElementException("Persongalleri mangler for sak $sakId")

        return Opplysningsbehov(
            sakid = sakId,
            sakType = behandling.sak.sakType,
            persongalleri = persongalleri
        )
    }
}