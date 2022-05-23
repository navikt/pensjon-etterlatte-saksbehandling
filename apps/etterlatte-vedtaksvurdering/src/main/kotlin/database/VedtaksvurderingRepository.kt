package no.nav.etterlatte.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.ZoneId
import java.util.*
import javax.sql.DataSource

class VedtaksvurderingRepository(private val datasource: DataSource) {

    private val logger = LoggerFactory.getLogger(VedtaksvurderingRepository::class.java)
    private val connection get() = datasource.connection
    private val postgresTimeZone = ZoneId.of("UTC")

    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository {
            return VedtaksvurderingRepository(datasource)
        }
    }

    fun lagreVilkaarsresultat(sakId: String, behandlingsId: UUID, vilkaarsresultat: VilkaarResultat) {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreVilkaarResultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.execute()
        }
    }

    fun oppdaterVilkaarsresultat(sakId: String, behandlingsId: UUID, vilkaarsresultat: VilkaarResultat) {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterVilkaarResultat)
            statement.setString(1, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    fun lagreKommerSoekerTilgodeResultat(sakId: String, behandlingsId: UUID, kommerSoekerTilgodeResultat: VilkaarResultat) {
        logger.info("Lagrer kommerSoekerTilgodeResultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreKommerSoekerTilgodeResultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(kommerSoekerTilgodeResultat))
            statement.execute()
        }
    }

    fun oppdaterKommerSoekerTilgodeResultat(sakId: String, behandlingsId: UUID, kommerSoekerTilgodeResultat: VilkaarResultat) {
        logger.info("Lagrer kommerSoekerTilgodeResultat")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdatereKommerSoekerTilgodeResultat)
            statement.setString(1, objectMapper.writeValueAsString(kommerSoekerTilgodeResultat))
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    fun lagreBeregningsresultat(sakId: String, behandlingsId: UUID, beregningsresultat: BeregningsResultat) {
        logger.info("Lagrer beregningsresultat")

        connection.use {
            val statement = it.prepareStatement(Queries.lagreBeregningsresultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(beregningsresultat))
            statement.execute()
        }
    }

    fun oppdaterBeregningsgrunnlag(sakId: String, behandlingsId: UUID, beregningsresultat: BeregningsResultat) {
        logger.info("Lagrer beregningsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterBeregningsresultat)
            statement.setString(1, objectMapper.writeValueAsString(beregningsresultat))
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    fun lagreAvkorting(sakId: String, behandlingsId: UUID, avkortingsResultat: String) {
        logger.info("Lagrer avkorting")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreAvkortingsresultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, avkortingsResultat)
            statement.execute()
        }
    }

    fun oppdaterAvkorting(sakId: String, behandlingsId: UUID, avkortingsResultat: String) {
        logger.info("Lagrer avkorting")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterAvkortingsresultat)
            statement.setString(1, avkortingsResultat)
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    fun hentVedtak(sakId: String, behandlingsId: UUID): Vedtak? {
        val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentVedtak)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.executeQuery().singleOrNull {
                Vedtak(
                    getString(1),
                    getObject(2) as UUID,
                    getString(3),
                    getString(4)?.let { objectMapper.readValue(it) },
                    getString(5)?.let { objectMapper.readValue(it) },
                    getString(6)?.let { objectMapper.readValue(it) },
                    getString(7)?.let { objectMapper.readValue(it) },
                    getBoolean(8)
                )
            }
        }
        return resultat
    }

    fun fattVedtak(saksbehandlerId: String, sakId: String, behandlingsId: UUID) {
        connection.use {
            val statement = it.prepareStatement(Queries.fattVedtak)
            statement.setString(1, saksbehandlerId)
            statement.setBoolean(2, true)
            statement.setLong(3, sakId.toLong())
            statement.setObject(4, behandlingsId)
            statement.execute()
        }
    }
}

data class Vedtak(
    val sakId: String,
    val behandlingId: UUID,
    val saksbehandlerId: String?,
    val avkortingsResultat: String?,
    val beregningsResultat: BeregningsResultat?,
    val vilkaarsResultat: VilkaarResultat?,
    val kommerSoekerTilgodeResultat: VilkaarResultat?,
    val vedtakFattet: Boolean?
)

private object Queries {
    val lagreBeregningsresultat = "INSERT INTO vedtak(sakId, behandlingId, beregningsresultat) VALUES (?, ?, ?)"
    val oppdaterBeregningsresultat = "UPDATE vedtak SET beregningsresultat = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreVilkaarResultat = "INSERT INTO vedtak(sakId, behandlingId, vilkaarsresultat) VALUES (?, ?, ?) "
    val oppdaterVilkaarResultat = "UPDATE vedtak SET vilkaarsresultat = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreKommerSoekerTilgodeResultat = "INSERT INTO vedtak(sakId, behandlingId, kommersoekertilgoderesultat) VALUES (?, ?, ?)"
    val oppdatereKommerSoekerTilgodeResultat = "UPDATE vedtak SET kommersoekertilgoderesultat = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreAvkortingsresultat = "INSERT INTO vedtak(sakId, behandlingId, avkortingsresultat) VALUES (?, ?, ?)"
    val oppdaterAvkortingsresultat = "UPDATE vedtak SET avkortingsresultat = ? WHERE sakId = ? AND behandlingId = ?"

    val fattVedtak = "UPDATE vedtak SET saksbehandlerId = ?, vedtakfattet = ? WHERE sakId = ? AND behandlingId = ?"
    val hentVedtak = "SELECT sakId, behandlingId, saksbehandlerId, avkortingsresultat, beregningsresultat, vilkaarsresultat, kommersoekertilgoderesultat, vedtakfattet FROM vedtak WHERE sakId = ? AND behandlingId = ?"

}

fun <T> ResultSet.singleOrNull(block: ResultSet.() -> T): T? {
    return if (next()) {
        block().also {
            require(!next()) { "Skal være unik" }
        }
    } else {
        null
    }
}

fun <T> ResultSet.toList(block: ResultSet.() -> T): List<T> {
    return generateSequence {
        if (next()) block()
        else null
    }.toList()
}