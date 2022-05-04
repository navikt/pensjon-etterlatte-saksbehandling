package no.nav.etterlatte.database

import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import org.slf4j.LoggerFactory
import java.sql.ResultSet
import java.time.ZoneId
import javax.sql.DataSource

class VedtaksvurderingRepository(private val datasource: DataSource) {

    private val logger = LoggerFactory.getLogger(VedtaksvurderingRepository::class.java)
    private val connection = datasource.connection
    private val postgresTimeZone = ZoneId.of("UTC")

    companion object {
        fun using(datasource: DataSource): VedtaksvurderingRepository {
            return VedtaksvurderingRepository(datasource)
        }
    }

    fun lagreVilkaarsresultat(sakId: String, behandlingsId: String, vilkaarsresultat: VilkaarResultat) {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreVilkaarResultat)
            statement.setString(0, sakId)
            statement.setString(1, behandlingsId)
            statement.setString(2, objectMapper.writeValueAsString(vilkaarsresultat))
            statement.execute()
        }
    }

    fun lagreBeregningsresultat(sakId: String, behandlingsId: String, beregningsresultat: BeregningsResultat) {
        logger.info("Lagrer beregningsresultat")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreBeregningsresultat)
            statement.setString(0, sakId)
            statement.setString(1, behandlingsId)
            statement.setString(2, objectMapper.writeValueAsString(beregningsresultat))
            statement.execute()
        }
    }

    fun lagreAvkorting(sakId: String, behandlingsId: String, avkortingsResultat: Any) {
        logger.info("Lagrer avkorting")
        connection.use {
            val statement = it.prepareStatement(Queries.lagreAvkortingsresultat)
            statement.setString(0, sakId)
            statement.setString(1, behandlingsId)
            statement.setString(2, objectMapper.writeValueAsString(avkortingsResultat))
            statement.execute()
        }
    }

    fun hentVedtak(sakId: String, behandlingsId: String): Vedtak? {
        val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentVedtak)
            statement.setString(0, sakId)
            statement.setString(1, behandlingsId)
            statement.executeQuery().singleOrNull {
                Vedtak(
                    getString(0),
                    getString(1),
                    getString(2),
                    getString(3),
                    objectMapper.readValue(getString(4), BeregningsResultat::class.java),
                    objectMapper.readValue(getString(5), VilkaarResultat::class.java),
                    getBoolean(6)
                )
            }
        }
        return resultat
    }

    fun hentVilkaarsresultat(sakId: String, behandlingsId: String): VilkaarResultat? {
        logger.info("henter vilkaarsresultat for $behandlingsId på sak $sakId")

        val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentVilkaarsresultat)
            statement.setString(0, sakId)
            statement.setString(1, behandlingsId)
            statement.executeQuery().singleOrNull {
                getString(0).let { objectMapper.readValue(it, VilkaarResultat::class.java) }
            }
        }
        return resultat
    }

    fun hentBeregningsresultat(sakId: String, behandlingsId: String): BeregningsResultat? {
        logger.info("henter beregningsresultat for $behandlingsId på sak $sakId")
        val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentBeregningsresultat)
            statement.setString(0, sakId)
            statement.setString(1, behandlingsId)
            statement.executeQuery().singleOrNull {
                getString(0).let { objectMapper.readValue(it, BeregningsResultat::class.java) }
            }
        }
        return resultat
    }

    fun hentAvkorting(sakId: String, behandlingsId: String): String {
        logger.info("henter avkortingsresultat for $behandlingsId på sak $sakId")
        /*val resultat = connection.use { it ->
            val statement = it.prepareStatement(Queries.hentAvkortingsresultat)
            statement.setString(0, sakId)
            statement.setString(1, behandlingsId)
            statement.executeQuery().singleOrNull {
                getString(0).let { objectMapper.readValue(it, ::class.java) }
            }
        }
        return resultat*/
        return ""
    }

    fun fattVedtak(saksbehandlerId: String, sakId: String, behandlingsId: String) {
        connection.use { it ->
            val statement = it.prepareStatement(Queries.fattVedtak)
            statement.setString(0, saksbehandlerId,)
            statement.setBoolean(1, true)
            statement.setString(2, sakId)
            statement.setString(3, behandlingsId)
            statement.execute()
        }
    }
}

data class Vedtak(
    val sakId: String,
    val behandlingId: String,
    val saksbehandlerId: String,
    val avkortingsResultat: String,
    val beregningsresultat: BeregningsResultat,
    val vilkaarsresultat: VilkaarResultat,
    val vedtakFattet: Boolean
)

private object Queries {
    val lagreBeregningsresultat = "INSERT INTO vedtak (sakId, behandlingId, beregningsresultat) VALUES (?, ?, ?) WHERE "
    val lagreVilkaarResultat = "INSERT INTO vedtak (sakId, behandlingId, vilkaarresultat) VALUES (?, ?, ?) WHERE "
    val lagreAvkortingsresultat = "INSERT INTO vedtak (sakId, behandlingId, avkortingsresultat) VALUES (?, ?, ?) WHERE "
    val fattVedtak = "UPDATE vedtak SET saksbehandlerId = ?, vedtakfattet = ? WHERE sakId = ? AND behandlingId = ?"

    val hentVedtak = "SELECT sakId, behandlingId, saksbehandlerId, avkortingsresultat, vilkaarsresultat, beregningsresultat, vedtakfattet FROM vedtak WHERE sakId = ? AND behandlingId = ?"
    val hentBeregningsresultat = "SELECT beregningsresultat FROM vedtak WHERE sakId = ? AND behandlingId = ?"
    val hentVilkaarsresultat = "SELECT vilkaarsresultat FROM vedtak WHERE sakId = ? AND behandlingId = ?"
    val hentAvkortingsresultat = "SELECT avkortingsresultat FROM vedtak WHERE sakId = ? AND behandlingId = ?"

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