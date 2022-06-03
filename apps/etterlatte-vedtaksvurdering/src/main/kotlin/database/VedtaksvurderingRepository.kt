package no.nav.etterlatte.database

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
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

    fun lagreVilkaarsresultat(sakId: String, behandlingsId: UUID, fnr: String, vilkaarsresultat: VilkaarResultat, virkningsDato: LocalDate) {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreVilkaarResultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.setString(4, fnr)
            statement.setDate(5, java.sql.Date.valueOf(virkningsDato))
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

    fun lagreKommerSoekerTilgodeResultat(sakId: String, behandlingsId: UUID, fnr: String, kommerSoekerTilgodeResultat: KommerSoekerTilgode) {
        logger.info("Lagrer kommerSoekerTilgodeResultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreKommerSoekerTilgodeResultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(kommerSoekerTilgodeResultat))
            statement.setString(4, fnr)
            statement.execute()
        }
    }

    fun oppdaterKommerSoekerTilgodeResultat(sakId: String, behandlingsId: UUID, kommerSoekerTilgodeResultat: KommerSoekerTilgode) {
        logger.info("Lagrer kommerSoekerTilgodeResultat")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdatereKommerSoekerTilgodeResultat)
            statement.setString(1, objectMapper.writeValueAsString(kommerSoekerTilgodeResultat))
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    fun lagreBeregningsresultat(sakId: String, behandlingsId: UUID, fnr: String, beregningsresultat: BeregningsResultat) {
        logger.info("Lagrer beregningsresultat")

        connection.use {
            val statement = it.prepareStatement(Queries.lagreBeregningsresultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(beregningsresultat))
            statement.setString(4, fnr)
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

    fun lagreAvkorting(sakId: String, behandlingsId: UUID, fnr: String, avkortingsResultat: AvkortingsResultat) {
        logger.info("Lagrer avkorting")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreAvkortingsresultat)
            statement.setLong(1, sakId.toLong())
            statement.setObject(2, behandlingsId)
            statement.setString(3, objectMapper.writeValueAsString(avkortingsResultat))
            statement.setString(4, fnr)
            statement.execute()
        }
    }

    fun oppdaterAvkorting(sakId: String, behandlingsId: UUID, avkortingsResultat: AvkortingsResultat) {
        logger.info("Lagrer avkorting")
        connection.use {
            val statement = it.prepareStatement(Queries.oppdaterAvkortingsresultat)
            statement.setString(1, objectMapper.writeValueAsString(avkortingsResultat))
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
                    getLong(9),
                    getString(1),
                    getObject(2) as UUID,
                    getString(3),
                    getString(4)?.let { try {
                        objectMapper.readValue(it)
                    } catch (ex: Exception){
                        null
                    } },
                    getJsonObject(5),
                    getJsonObject(6),
                    getJsonObject(7),
                    getBoolean(8),
                    getString(10),
                    getTimestamp(11)?.toInstant(),
                    getTimestamp(12)?.toInstant(),
                    getString(13),
                    getDate(14)?.toLocalDate()
                )
            }
        }
        return resultat
    }

    private inline fun <reified T> ResultSet.getJsonObject(c: Int):T? {
        return getString(c)?.let { try {
            objectMapper.readValue(it)
        } catch (ex: Exception){
            logger.warn("vedtak ${getLong("id")} kan ikke lese kolonne $c")
            null
        }

        }
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

    fun attesterVedtak(saksbehandlerId: String, sakId: String, behandlingsId: UUID) {
        connection.use {
            val statement = it.prepareStatement(Queries.attesterVedtak)
            statement.setString(1, saksbehandlerId)
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingsId)
            statement.execute()
        }
    }

    fun lagreFnr(sakId: String, behandlingId: UUID, fnr: String) {
        connection.use {
            val statement = it.prepareStatement(Queries.lagreFnr)
            statement.setString(1, fnr)
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingId)
            statement.execute()
        }
    }

    fun lagreDatoVirk(sakId: String, behandlingId: UUID, datoVirk: LocalDate) {
        connection.use {
            val statement = it.prepareStatement(Queries.lagreDatoVirkFom)
            statement.setDate(1, java.sql.Date.valueOf(datoVirk))
            statement.setLong(2, sakId.toLong())
            statement.setObject(3, behandlingId)
            statement.execute()
        }
    }
}

data class Vedtak(
    val id: Long,
    val sakId: String,
    val behandlingId: UUID,
    val saksbehandlerId: String?,
    val avkortingsResultat: AvkortingsResultat?,
    val beregningsResultat: BeregningsResultat?,
    val vilkaarsResultat: VilkaarResultat?,
    val kommerSoekerTilgodeResultat: KommerSoekerTilgode?,
    val vedtakFattet: Boolean?,
    val fnr: String?,
    val datoFattet: Instant?,
    val datoattestert: Instant?,
    val attestant: String?,
    val virkningsDato: LocalDate?,
)

private object Queries {
    val lagreBeregningsresultat = "INSERT INTO vedtak(sakId, behandlingId, beregningsresultat, fnr) VALUES (?, ?, ?, ?)"
    val oppdaterBeregningsresultat = "UPDATE vedtak SET beregningsresultat = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreVilkaarResultat = "INSERT INTO vedtak(sakId, behandlingId, vilkaarsresultat, datoVirkFom) VALUES (?, ?, ?, ?, ?) "
    val oppdaterVilkaarResultat = "UPDATE vedtak SET vilkaarsresultat = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreKommerSoekerTilgodeResultat = "INSERT INTO vedtak(sakId, behandlingId, kommersoekertilgoderesultat, fnr) VALUES (?, ?, ?, ?)"
    val oppdatereKommerSoekerTilgodeResultat = "UPDATE vedtak SET kommersoekertilgoderesultat = ? WHERE sakId = ? AND behandlingId = ?"

    val lagreAvkortingsresultat = "INSERT INTO vedtak(sakId, behandlingId, avkortingsresultat, fnr) VALUES (?, ?, ?, ?)"
    val oppdaterAvkortingsresultat = "UPDATE vedtak SET avkortingsresultat = ? WHERE sakId = ? AND behandlingId = ?"

    val fattVedtak = "UPDATE vedtak SET saksbehandlerId = ?, vedtakfattet = ?, datoFattet = now() WHERE sakId = ? AND behandlingId = ?"
    val attesterVedtak = "UPDATE vedtak SET attestant = ?, datoAttestert = now() WHERE sakId = ? AND behandlingId = ?"
    val hentVedtak = "SELECT sakId, behandlingId, saksbehandlerId, avkortingsresultat, beregningsresultat, vilkaarsresultat, kommersoekertilgoderesultat, vedtakfattet, id, fnr, datoFattet, datoattestert, attestant, datoVirkFom FROM vedtak WHERE sakId = ? AND behandlingId = ?"

    val lagreFnr = "UPDATE vedtak SET fnr = ? WHERE sakId = ? AND behandlingId = ?"
    val lagreDatoVirkFom = "UPDATE vedtak SET datoVirkFom = ? WHERE sakId = ? AND behandlingId = ?"
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