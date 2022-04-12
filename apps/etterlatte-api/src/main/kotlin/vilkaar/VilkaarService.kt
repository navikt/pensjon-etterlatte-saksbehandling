package no.nav.etterlatte.vilkaar

import no.nav.etterlatte.model.VurdertVilkaar
import org.slf4j.LoggerFactory

class VilkaarService(private val vilkaarKlient: VilkaarKlient) {
    private val logger = LoggerFactory.getLogger(VilkaarService::class.java)

    suspend fun hentVurdertVilkaar(behandlingId: String, accessToken: String): VurdertVilkaar? {
        logger.info("Henter vurdert vilkaar for $behandlingId")
        return vilkaarKlient.hentVurdertVilkaar(behandlingId, accessToken)
    }

}