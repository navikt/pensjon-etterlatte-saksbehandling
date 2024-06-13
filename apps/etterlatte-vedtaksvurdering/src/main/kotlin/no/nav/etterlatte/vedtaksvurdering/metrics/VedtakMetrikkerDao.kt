package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.database.toList
import java.sql.ResultSet
import javax.sql.DataSource

class VedtakMetrikkerDao(
    private val dataSource: DataSource,
) {
    companion object {
        fun using(datasource: DataSource): VedtakMetrikkerDao = VedtakMetrikkerDao(datasource)
    }

    fun hentLoependeYtelseAntall(): List<VedtakAntall> =
        dataSource.connection.use {
            val statement =
                it.prepareStatement(
                    """
                    select count(*) antall, saktype from vedtak
                    where type = 'INNVILGELSE' and vedtakstatus = 'IVERKSATT'
                    and not vedtak.sakid in (
                        select sakid from vedtak where type = 'OPPHOER' and vedtakstatus = 'IVERKSATT'
                    )
                    group by saktype
                    """.trimIndent(),
                )
            return statement.executeQuery().toList {
                asVedtakAntall()
            }
        }
}

data class VedtakAntall(
    val antall: Int,
    val sakType: SakType,
)

fun ResultSet.asVedtakAntall() =
    VedtakAntall(
        antall = getInt("antall"),
        sakType = SakType.valueOf(getString("saktype")),
    )
