package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering.metrics

import javax.sql.DataSource

class VedtakMetrikkerDao(private val dataSource: DataSource) {
    companion object {
        fun using(datasource: DataSource): VedtakMetrikkerDao = VedtakMetrikkerDao(datasource)
    }
}
