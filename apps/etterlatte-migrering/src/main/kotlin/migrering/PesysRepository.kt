package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.KotliqueryRepository
import java.util.*

internal class PesysRepository(private val repository: KotliqueryRepository) {

    fun hentSaker(): List<Pesyssak> = repository.hentListeMedKotliquery(
        "SELECT sak from pesyssak WHERE migrert is false"
    ) {
        tilPesyssak(it.string("sak"))
    }

    private fun tilPesyssak(sak: String) = objectMapper.readValue(sak, Pesyssak::class.java)

    fun lagrePesyssak(pesyssak: Pesyssak) =
        repository.opprett(
            "INSERT INTO pesyssak(id,sak,migrert) VALUES(:id,:sak::jsonb,:migrert::boolean)",
            mapOf("id" to UUID.randomUUID(), "sak" to pesyssak.toJson(), "migrert" to false),
            "Lagra pesyssak ${pesyssak.pesysId} i migreringsbasen"
        )

    fun settSakMigrert(id: UUID) = repository.oppdater(
        "UPDATE pesyssak SET migrert=:migrert WHERE id=:id",
        mapOf("id" to id, "migrert" to true),
        "Markerte $id som migrert"
    )
}