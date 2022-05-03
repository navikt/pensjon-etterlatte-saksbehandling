package no.nav.etterlatte.database

import org.slf4j.LoggerFactory
import java.time.ZoneId
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

    fun lagreVilkaarsresultat() {
        logger.info("Lagrer vilkaarsresultat")
        connection.use {
            it.prepareStatement(Queries.lagreBeregningsresultat)
        }
    }

    fun lagreBeregningsresultat() {
        logger.info("Lagrer beregningsresultat")
    }

    fun lagreAvkorting() {
        logger.info("Lagrer avkorting")
    }


}

private object Queries {
    val lagreBeregningsresultat = "INSERT INTO beregningsresultat () VALUES ()"
}