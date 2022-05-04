package no.nav.etterlatte.db

import no.nav.etterlatte.db.BrevRepository.Queries.HENT_BREV_QUERY
import no.nav.etterlatte.db.BrevRepository.Queries.OPPDATER_BREV_QUERY
import no.nav.etterlatte.db.BrevRepository.Queries.OPPRETT_BREV_QUERY
import java.sql.ResultSet
import javax.sql.DataSource

typealias BrevID = Long

class Brev(
    val id: BrevID,
    val vedtakId: Long,
    val data: ByteArray
)

class BrevRepository private constructor(private val ds: DataSource) {

    private val connection get() = ds.connection

    fun hentBrev(vedtakId: Long): Brev? = connection.use {
        it.prepareStatement(HENT_BREV_QUERY)
            .apply {
                setLong(1, vedtakId)
            }
            .executeQuery()
            .singleOrNull {
                Brev(getLong("id"), getLong("vedtak_id"), getBytes("pdf"))
            }
    }

    fun opprettBrev(vedtakId: Long, pdf: ByteArray): Brev = connection.use {
        val id = it.prepareStatement(OPPRETT_BREV_QUERY)
            .apply {
                setLong(1, vedtakId)
                setBytes(2, pdf)
            }
            .executeQuery()
            .singleOrNull { getLong(1) }!!

        Brev(id, vedtakId, pdf)
    }

    fun oppdaterBrev(vedtakId: Long, pdf: ByteArray): Brev = connection.use {
        val id = it.prepareStatement(OPPDATER_BREV_QUERY)
            .apply {
                setLong(1, vedtakId)
                setBytes(2, pdf)
            }
            .executeQuery()
            .singleOrNull { getLong(1) }!!

        Brev(id, vedtakId, pdf)
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

    companion object {
        fun using(datasource: DataSource): BrevRepository {
            return BrevRepository(datasource)
        }
    }

    private object Queries {
        const val HENT_BREV_QUERY = "SELECT * FROM brev WHERE vedtak_id = ?"

        const val OPPDATER_BREV_QUERY = "UPDATE brev SET pdf = ? WHERE vedtak_id = ? RETURNING id"

        const val OPPRETT_BREV_QUERY = "INSERT INTO brev (vedtak_id, pdf) VALUES (?, ?) RETURNING id"
    }

}
