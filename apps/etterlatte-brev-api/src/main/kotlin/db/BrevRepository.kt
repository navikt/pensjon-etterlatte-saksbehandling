package no.nav.etterlatte.db

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.db.BrevRepository.Queries.HENT_ALLE_BREV_QUERY
import no.nav.etterlatte.db.BrevRepository.Queries.HENT_BREV_QUERY
import no.nav.etterlatte.db.BrevRepository.Queries.HENT_SISTE_STATUS
import no.nav.etterlatte.db.BrevRepository.Queries.OPPDATER_STATUS_QUERY
import no.nav.etterlatte.db.BrevRepository.Queries.OPPRETT_BREV_QUERY
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import java.sql.ResultSet
import javax.sql.DataSource

typealias BrevID = Long

@JsonIgnoreProperties(ignoreUnknown = true)
data class Adresse(
    val adresse: String,
    val postnummer: String,
    val poststed: String,
    val land: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mottaker(
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: Foedselsnummer? = null,
    val adresse: Adresse
)

class Brev(
    val id: BrevID,
    val behandlingId: Long,
    val status: String,
    val mottaker: Mottaker,
    val data: ByteArray? = null
)

class BrevRepository private constructor(private val ds: DataSource) {

    private val connection get() = ds.connection

    fun hentBrev(id: BrevID): Brev = connection.use {
        it.prepareStatement(HENT_BREV_QUERY)
            .apply { setLong(1, id) }
            .executeQuery()
            .singleOrNull {
                Brev(
                    id = getLong("id"),
                    behandlingId = getLong("behandling_id"),
                    status = getString("status_id"),
                    Mottaker(
                        fornavn = getString("fornavn"),
                        etternavn = getString("etternavn"),
//                        foedselsnummer = getString("foedselsnummer"),
                        adresse = Adresse(
                            adresse = getString("adresse"),
                            postnummer = getString("postnummer"),
                            poststed = getString("poststed")
                        )
                    ),
                    data = getBytes("bytes")
                )
            }!!
    }

    fun hentBrevInnhold(id: BrevID): ByteArray = connection.use {
        it.prepareStatement("SELECT bytes FROM innhold WHERE brev_id = ?")
            .apply { setLong(1, id) }
            .executeQuery()
            .singleOrNull { getBytes("bytes") }!!
    }

    fun hentBrevForBehandling(behandlingId: Long): List<Brev> = connection.use {
        it.prepareStatement(HENT_ALLE_BREV_QUERY)
            .apply {
                setLong(1, behandlingId)
            }
            .executeQuery()
            .toList {
                Brev(
                    id = getLong("id"),
                    behandlingId = getLong("behandling_id"),
                    status = getString("status_id"),
                    Mottaker(
                        fornavn = getString("fornavn"),
                        etternavn = getString("etternavn"),
//                        foedselsnummer = getString("foedselsnummer"),
                        adresse = Adresse(
                            adresse = getString("adresse"),
                            postnummer = getString("postnummer"),
                            poststed = getString("poststed")
                        )
                    )
                )
            }
    }

    fun opprettBrev(behandlingId: Long, mottaker: Mottaker, pdf: ByteArray? = null): Brev = connection.use {
        val id = it.prepareStatement(OPPRETT_BREV_QUERY)
            .apply {
                setLong(1, behandlingId)
                setString(2, mottaker.fornavn)
                setString(3, mottaker.etternavn)
                setString(4, mottaker.adresse.adresse)
                setString(5, mottaker.adresse.postnummer)
                setString(6, mottaker.adresse.poststed)
            }
            .executeQuery()
            .singleOrNull { getLong(1) }!!

        if (pdf != null) {
            val inserted = it.prepareStatement("INSERT INTO innhold (brev_id, malnavn, spraak, bytes) VALUES (?, ?, ?, ?)")
                .apply {
                    setLong(1, id)
                    setString(2, "navn på malen")
                    setString(3, "nb")
                    setBytes(4, pdf)
                }
                .executeUpdate()

            if (inserted < 1) throw RuntimeException()
        }

        val status = "OPPRETTET"
        oppdaterStatus(id, status)

        Brev(id, behandlingId, status, mottaker, pdf)
    }

    fun oppdaterStatus(id: BrevID, status: String, payload: String? = null) = connection.use {
        it.prepareStatement(OPPDATER_STATUS_QUERY)
            .apply {
                setLong(1, id)
                setString(2, status)
                setString(3, payload ?: "{}")
            }
            .executeUpdate()
    }

    fun hentSisteStatus(id: BrevID): String = connection.use {
        it.prepareStatement(HENT_SISTE_STATUS)
            .apply { setLong(1, id) }
            .executeQuery()
            .singleOrNull { getString(1) }!!
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

    companion object {
        fun using(datasource: DataSource): BrevRepository {
            return BrevRepository(datasource)
        }
    }

    private object Queries {
        const val HENT_BREV_QUERY = """
            SELECT b.id, b.behandling_id, h.status_id, m.*
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
            SELECT b.id, b.behandling_id, h.status_id, m.*
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
                INSERT INTO brev (behandling_id) VALUES (?) RETURNING id
            ) INSERT INTO mottaker (brev_id, fornavn, etternavn, adresse, postnummer, poststed)
                VALUES ((SELECT id FROM nytt_brev), ?, ?, ?, ?, ?) RETURNING brev_id
        """

        const val OPPDATER_STATUS_QUERY = """
            INSERT INTO hendelse (brev_id, status_id, payload) VALUES (?, ?, ?)
        """
    }

}
