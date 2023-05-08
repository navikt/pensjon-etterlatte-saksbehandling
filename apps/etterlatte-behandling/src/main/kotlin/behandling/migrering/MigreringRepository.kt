package no.nav.etterlatte.behandling.migrering

import no.nav.etterlatte.libs.database.KotliqueryRepository
import no.nav.etterlatte.rapidsandrivers.migrering.PesysId

class MigreringRepository(private val kotliqueryRepository: KotliqueryRepository) {
    fun lagreKoplingTilPesyssaka(pesysSakId: PesysId, sakId: Long) = kotliqueryRepository.opprett(
        "INSERT INTO pesyskopling(pesys_id,sak_id) VALUES(:pesysSakId, :sakId)",
        mapOf("pesysSakId" to pesysSakId.id, "sakId" to sakId),
        "Oppretta kopling fra sak $sakId til pesyssak $pesysSakId"
    )
}