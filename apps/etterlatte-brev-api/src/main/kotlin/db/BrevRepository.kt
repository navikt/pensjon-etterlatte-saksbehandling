package no.nav.etterlatte.db

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.etterlatte.db.BrevRepository.Queries.HENT_BREV_QUERY
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
    val mottaker: Mottaker,
    val data: ByteArray? = null
)

class BrevRepository private constructor(private val ds: DataSource) {

    private val connection get() = ds.connection

    fun hentBrev(id: BrevID): Brev = connection.use {
        it.prepareStatement("SELECT * FROM brev WHERE id = ?")
            .apply { setLong(1, id) }
            .executeQuery()
            .singleOrNull {
                Brev(
                    id = getLong("id"),
                    behandlingId = getLong("behandling_id"),
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
            }!!
    }

    fun hentBrevForBehandling(behandlingId: Long): List<Brev> = connection.use {
        it.prepareStatement(HENT_BREV_QUERY)
            .apply {
                setLong(1, behandlingId)
            }
            .executeQuery()
            .toList {
                Brev(
                    id = getLong("id"),
                    behandlingId = getLong("behandling_id"),
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

    fun opprettBrev(behandlingId: Long, mottaker: Mottaker): Brev = connection.use {
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

        Brev(id, behandlingId, mottaker)
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
            SELECT * 
            FROM brev 
            INNER JOIN mottaker m on brev.id = m.brev_id
            WHERE behandling_id = ?
        """

        const val OPPRETT_BREV_QUERY = """
            WITH nytt_brev AS (
                INSERT INTO brev (behandling_id) VALUES (?) RETURNING id
            ) INSERT INTO mottaker (brev_id, fornavn, etternavn, adresse, postnummer, poststed)
                VALUES ((SELECT id FROM nytt_brev), ?, ?, ?, ?, ?) RETURNING brev_id
        """
    }

}
