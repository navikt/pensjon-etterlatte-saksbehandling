package no.nav.etterlatte.behandling.migrering

import no.nav.etterlatte.libs.database.KotliqueryRepositoryWrapper
import rapidsandrivers.migrering.PesysId

class MigreringRepository(private val kotliqueryRepositoryWrapper: KotliqueryRepositoryWrapper) {
    fun lagreKoplingTilPesyssaka(pesysSakId: PesysId, sakId: Long) = kotliqueryRepositoryWrapper.opprett(
        "INSERT INTO pesyskopling(pesys_id,sak_id) VALUES(:pesysSakId, :sakId)",
        mapOf("pesysSakId" to pesysSakId.id, "sakId" to sakId),
        "Oppretta kopling fra sak $sakId til pesyssak $pesysSakId"
    )
}