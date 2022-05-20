package no.nav.etterlatte.db

import no.nav.etterlatte.db.BrevRepository.Queries.HENT_ALLE_BREV_QUERY
import no.nav.etterlatte.db.BrevRepository.Queries.HENT_BREV_QUERY
import no.nav.etterlatte.db.BrevRepository.Queries.OPPDATER_STATUS_QUERY
import no.nav.etterlatte.db.BrevRepository.Queries.OPPRETT_BREV_QUERY
import java.sql.ResultSet
import javax.sql.DataSource

class BrevRepository private constructor(private val ds: DataSource) {

    private val connection get() = ds.connection

    fun hentBrev(id: BrevID): Brev = connection.use {
        it.prepareStatement(HENT_BREV_QUERY)
            .apply { setLong(1, id) }
            .executeQuery()
            .singleOrNull { mapTilBrev() }!!
    }

    fun hentBrevInnhold(id: BrevID): BrevInnhold = connection.use {
        it.prepareStatement("SELECT * FROM innhold WHERE brev_id = ?")
            .apply { setLong(1, id) }
            .executeQuery()
            .singleOrNull {
                BrevInnhold(
                    getString("mal"),
                    getString("spraak"),
                    getBytes("bytes")
                )
            }!!
    }

    fun hentBrevForBehandling(behandlingId: Long): List<Brev> = connection.use {
        it.prepareStatement(HENT_ALLE_BREV_QUERY)
            .apply { setLong(1, behandlingId) }
            .executeQuery()
            .toList { mapTilBrev() }
    }

    fun opprettBrev(nyttBrev: NyttBrev): Brev =
        connection.use {
        val id = it.prepareStatement(OPPRETT_BREV_QUERY)
            .apply {
                setLong(1, nyttBrev.behandlingId)
                setString(2, nyttBrev.tittel)
                setString(3, nyttBrev.mottaker.fornavn)
                setString(4, nyttBrev.mottaker.etternavn)
                setString(5, nyttBrev.mottaker.adresse.adresse)
                setString(6, nyttBrev.mottaker.adresse.postnummer)
                setString(7, nyttBrev.mottaker.adresse.poststed)
            }
            .executeQuery()
            .singleOrNull { getLong(1) }!!

        // TODO: Lagre malnavn og språk
        val inserted = it.prepareStatement("INSERT INTO innhold (brev_id, mal, spraak, bytes) VALUES (?, ?, ?, ?)")
            .apply {
                setLong(1, id)
                setString(2, "navn på malen")
                setString(3, "nb")
                setBytes(4, nyttBrev.pdf)
            }
            .executeUpdate()

        if (inserted < 1) throw RuntimeException()

        oppdaterStatus(id, nyttBrev.status)

        Brev.fraNyttBrev(id, nyttBrev)
    }

    fun setJournalpostId(brevId: Long, journalpostId: String): Boolean = connection.use {
        it.prepareStatement("UPDATE brev SET journalpost_id = ? WHERE id = ?")
            .apply {
                setString(1, journalpostId)
                setLong(2, brevId)
            }
            .executeUpdate() > 0
    }

    fun setBestillingId(brevId: Long, bestillingId: String): Boolean = connection.use {
        it.prepareStatement("UPDATE brev SET bestilling_id = ? WHERE id = ?")
            .apply {
                setString(1, bestillingId)
                setLong(2, brevId)
            }
            .executeUpdate() > 0
    }

    fun oppdaterStatus(id: BrevID, status: Status, payload: String? = null) = connection.use {
        it.prepareStatement(OPPDATER_STATUS_QUERY)
            .apply {
                setLong(1, id)
                setString(2, status.name)
                setString(3, payload ?: "{}")
            }
            .executeUpdate()
    }

    fun slett(id: BrevID): Boolean = connection.use {
        it.prepareStatement("DELETE FROM brev WHERE id = ?")
            .apply { setLong(1, id) }
            .executeUpdate() > 0
    }

    private fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
        return if (next()) {
            block().also {
                require(!next()) { "Skal være unik" }
            }
        } else {
            null
        }
    }

    private fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
        return generateSequence {
            if (next()) block()
            else null
        }.toList()
    }

    private fun ResultSet.mapTilBrev() = Brev(
        id = getLong("id"),
        behandlingId = getLong("behandling_id"),
        tittel = getString("tittel"),
        status = Status.valueOf(getString("status_id")),
        Mottaker(
            fornavn = getString("fornavn"),
            etternavn = getString("etternavn"),
            adresse = Adresse(
                adresse = getString("adresse"),
                postnummer = getString("postnummer"),
                poststed = getString("poststed")
            )
        )
    )

    companion object {
        fun using(datasource: DataSource): BrevRepository {
            return BrevRepository(datasource)
        }
    }

    private object Queries {
        const val HENT_BREV_QUERY = """
            SELECT b.id, b.behandling_id, b.tittel, h.status_id, m.*
            FROM brev b
            INNER JOIN mottaker m on b.id = m.brev_id
            INNER JOIN hendelse h on b.id = h.brev_id
            WHERE b.id = ?
            AND h.id IN (
                SELECT DISTINCT ON (brev_id) id
                FROM hendelse
                ORDER  BY brev_id, opprettet DESC
            )
        """

        const val HENT_ALLE_BREV_QUERY = """
            SELECT b.id, b.behandling_id, b.tittel, h.status_id, m.*
            FROM brev b
            INNER JOIN mottaker m on b.id = m.brev_id
            INNER JOIN hendelse h on b.id = h.brev_id
            WHERE b.behandling_id = ?
            AND h.id IN (
                SELECT DISTINCT ON (brev_id) id
                FROM hendelse
                ORDER  BY brev_id, opprettet DESC
            )
        """

        const val HENT_SISTE_STATUS = """
            SELECT * FROM hendelse
            WHERE brev_id IN (
                SELECT MAX(opprettet)
            )
        """

        const val OPPRETT_BREV_QUERY = """
            WITH nytt_brev AS (
                INSERT INTO brev (behandling_id, tittel) VALUES (?, ?) RETURNING id
            ) INSERT INTO mottaker (brev_id, fornavn, etternavn, adresse, postnummer, poststed)
                VALUES ((SELECT id FROM nytt_brev), ?, ?, ?, ?, ?) RETURNING brev_id
        """

        const val OPPDATER_STATUS_QUERY = """
            INSERT INTO hendelse (brev_id, status_id, payload) VALUES (?, ?, ?)
        """
    }
}
