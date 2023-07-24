package no.nav.etterlatte.behandling

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.libs.common.opplysningsbehov.Opplysningsbehov

class GrunnlagService(private val grunnlagKlient: GrunnlagKlientImpl) {
    fun leggInnNyttGrunnlag(behandling: Behandling) {
        val grunnlagsbehov = grunnlagsbehov(behandling)
        runBlocking {
            grunnlagKlient.leggInnNyttGrunnlag(grunnlagsbehov)
        }
    }

    private fun grunnlagsbehov(behandling: Behandling): Opplysningsbehov {
        val persongalleri = behandling.persongalleri
        return Opplysningsbehov(
            sakid = behandling.sak.id,
            sakType = behandling.sak.sakType,
            persongalleri = persongalleri
        )
    }
}