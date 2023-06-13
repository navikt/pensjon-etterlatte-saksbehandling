package no.nav.etterlatte.migrering

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.database.hentListe
import no.nav.etterlatte.libs.database.oppdater
import no.nav.etterlatte.libs.database.opprett
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource

internal class PesysRepository(private val dataSource: DataSource) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentSaker(): List<Pesyssak> = dataSource.hentListe(
        "SELECT sak from pesyssak WHERE migrert is false"
    ) {
        tilPesyssak(it.string("sak"))
    }

    private fun tilPesyssak(sak: String) = objectMapper.readValue(sak, Pesyssak::class.java)

    fun lagrePesyssak(pesyssak: Pesyssak) =
        dataSource.opprett(
            "INSERT INTO pesyssak(id,sak,migrert) VALUES(:id,:sak::jsonb,:migrert::boolean)",
            mapOf("id" to UUID.randomUUID(), "sak" to pesyssak.toJson(), "migrert" to false),
            "Lagra pesyssak ${pesyssak.pesysId} i migreringsbasen",
            logger
        )

    fun settSakMigrert(id: UUID) = dataSource.oppdater(
        "UPDATE pesyssak SET migrert=:migrert WHERE id=:id",
        mapOf("id" to id, "migrert" to true),
        "Markerte $id som migrert",
        logger
    )
}