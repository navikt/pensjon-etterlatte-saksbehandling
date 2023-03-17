package no.nav.etterlatte.brev.db

import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_ALLE_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.HENT_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_INNHOLD_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_MOTTAKER_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPDATER_STATUS_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_BREV_QUERY
import no.nav.etterlatte.brev.db.BrevRepository.Queries.OPPRETT_INNHOLD_QUERY
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.database.singleOrNull
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import java.util.UUID
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
                    Spraak.valueOf(getString("spraak")),
                    getBytes("bytes")
                )
            }!!
    }

    fun hentBrevForBehandling(behandlingId: UUID): List<Brev> = connection.use {
        it.prepareStatement(HENT_ALLE_BREV_QUERY)
            .apply { setObject(1, behandlingId) }
            .executeQuery()
            .toList { mapTilBrev() }
    }

    fun oppdaterBrev(brevId: Long, brev: UlagretBrev) {
        val oppdatert = connection.use {
            it.prepareStatement(OPPDATER_BREV_QUERY)
                .apply {
                    setString(1, brev.tittel)
                    setBoolean(2, brev.erVedtaksbrev)
                    setLong(3, brevId)
                }
                .executeUpdate()

            it.prepareStatement(OPPDATER_INNHOLD_QUERY)
                .apply {
                    setString(1, "TODO: mal")
                    setString(2, brev.spraak.name)
                    setBytes(3, brev.pdf)
                    setLong(4, brevId)
                }
                .executeUpdate()

            it.prepareStatement(OPPDATER_MOTTAKER_QUERY)
                .apply {
                    setString(1, brev.mottaker.foedselsnummer?.value)
                    setString(2, brev.mottaker.orgnummer)
                    setString(3, brev.mottaker.adresse?.navn)
                    setString(4, brev.mottaker.adresse?.adresse)
                    setString(5, brev.mottaker.adresse?.postnummer)
                    setString(6, brev.mottaker.adresse?.poststed)
                    setString(7, brev.mottaker.adresse?.land)
                    setLong(8, brevId)
                }
                .executeUpdate()
        }

        if (oppdatert == 1) oppdaterStatus(brevId, Status.OPPDATERT)
    }

    fun opprettBrev(ulagretBrev: UlagretBrev): Brev {
        val id = connection.use {
            val id = it.prepareStatement(OPPRETT_BREV_QUERY)
                .apply {
                    setObject(1, ulagretBrev.behandlingId)
                    setString(2, ulagretBrev.soekerFnr)
                    setString(3, ulagretBrev.tittel)
                    setBoolean(4, ulagretBrev.erVedtaksbrev)
                    setString(5, ulagretBrev.mottaker.foedselsnummer?.value)
                    setString(6, ulagretBrev.mottaker.orgnummer)
                    setString(7, ulagretBrev.mottaker.adresse?.navn)
                    setString(8, ulagretBrev.mottaker.adresse?.adresse)
                    setString(9, ulagretBrev.mottaker.adresse?.postnummer)
                    setString(10, ulagretBrev.mottaker.adresse?.poststed)
                    setString(11, ulagretBrev.mottaker.adresse?.land)
                }
                .executeQuery()
                .singleOrNull { getLong(1) }!!

            // TODO: Lagre malnavn
            val inserted = it.prepareStatement(OPPRETT_INNHOLD_QUERY)
                .apply {
                    setLong(1, id)
                    setString(2, "navn på malen")
                    setString(3, ulagretBrev.spraak.name)
                    setBytes(4, ulagretBrev.pdf)
                }
                .executeUpdate()

            if (inserted < 1) {
                throw RuntimeException()
            } else {
                id
            }
        }

        oppdaterStatus(id, ulagretBrev.status)

        return Brev.fraUlagretBrev(id, ulagretBrev)
    }

    fun setJournalpostId(brevId: Long, journalpostId: String): Boolean = connection.use {
        it.prepareStatement("UPDATE brev SET journalpost_id = ? WHERE id = ?")
            .apply {
                setString(1, journalpostId)
                setLong(2, brevId)
            }
            .executeUpdate() > 0
    }

    fun setBestillingsId(brevId: Long, bestillingsId: String): Boolean = connection.use {
        it.prepareStatement("UPDATE brev SET bestilling_id = ? WHERE id = ?")
            .apply {
                setString(1, bestillingsId)
                setLong(2, brevId)
            }
            .executeUpdate() > 0
    }

    fun oppdaterStatus(id: BrevID, status: Status, payload: String? = null): Boolean = connection.use {
        it.prepareStatement(OPPDATER_STATUS_QUERY)
            .apply {
                setLong(1, id)
                setString(2, status.name)
                setString(3, payload ?: "{}")
            }
            .executeUpdate() > 0
    }

    fun slett(id: BrevID): Boolean = connection.use {
        it.prepareStatement("DELETE FROM brev WHERE id = ?")
            .apply { setLong(1, id) }
            .executeUpdate() > 0
    }

    private fun ResultSet.mapTilBrev() = Brev(
        id = getLong("id"),
        behandlingId = getObject("behandling_id") as UUID,
        soekerFnr = getString("soeker_fnr"),
        tittel = getString("tittel"),
        status = Status.valueOf(getString("status_id")),
        mottaker = Mottaker(
            foedselsnummer = getString("foedselsnummer")?.let { Foedselsnummer.of(it) },
            orgnummer = getString("orgnummer"),
            adresse = getString("navn")?.let {
                Adresse(
                    navn = it,
                    adresse = getString("adresse"),
                    postnummer = getString("postnummer"),
                    poststed = getString("poststed")
                )
            }
        ),
        erVedtaksbrev = getBoolean("vedtaksbrev")
    )

    companion object {
        fun using(datasource: DataSource): BrevRepository {
            return BrevRepository(datasource)
        }
    }

    private object Queries {
        const val HENT_BREV_QUERY = """
            SELECT b.id, b.behandling_id, b.soeker_fnr, b.tittel, b.vedtaksbrev, h.status_id, m.*
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
            SELECT b.id, b.behandling_id, b.soeker_fnr, b.tittel, b.vedtaksbrev, h.status_id, m.*
            FROM brev b
            INNER JOIN mottaker m on b.id = m.brev_id
            INNER JOIN hendelse h on b.id = h.brev_id
            WHERE b.behandling_id = ?
            AND h.id IN (
                SELECT DISTINCT ON (brev_id) id
                FROM hendelse
                ORDER BY brev_id, opprettet DESC
            )
        """

        const val OPPRETT_BREV_QUERY = """
            WITH nytt_brev AS (
                INSERT INTO brev (behandling_id, soeker_fnr, tittel, vedtaksbrev) VALUES (?, ?, ?, ?) RETURNING id
            ) 
            INSERT INTO mottaker (brev_id, foedselsnummer, orgnummer, navn, adresse, postnummer, poststed, land)
                VALUES ((SELECT id FROM nytt_brev), ?, ?, ?, ?, ?, ?, ?) RETURNING brev_id
        """

        const val OPPRETT_INNHOLD_QUERY = """
            INSERT INTO innhold (brev_id, mal, spraak, bytes) 
            VALUES (?, ?, ?, ?)
        """

        const val OPPDATER_INNHOLD_QUERY = """
            UPDATE innhold 
            SET mal = ?, spraak = ?, bytes = ?
            WHERE brev_id = ?
        """

        const val OPPDATER_MOTTAKER_QUERY = """
            UPDATE mottaker 
            SET foedselsnummer = ?, orgnummer = ?, navn = ?, adresse = ?, postnummer = ?, poststed = ?, land = ?
            WHERE brev_id = ?
        """

        const val OPPDATER_BREV_QUERY = """
            UPDATE brev 
            SET tittel = ?, vedtaksbrev = ? 
            WHERE id = ?
        """

        const val OPPDATER_STATUS_QUERY = """
            INSERT INTO hendelse (brev_id, status_id, payload) 
            VALUES (?, ?, ?)
        """
    }
}