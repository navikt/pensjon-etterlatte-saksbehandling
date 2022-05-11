package no.nav.etterlatte

import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.rivers.LagreAvkorting
import no.nav.etterlatte.rivers.LagreBeregningsresultat
import no.nav.etterlatte.rivers.LagreVilkaarsresultat
import no.nav.helse.rapids_rivers.RapidApplication

fun main() {

    val ds = DataSourceBuilder(System.getenv())
    ds.migrate()

    val vedtakRepo = VedtaksvurderingRepository.using(ds.dataSource)
    val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)

    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->

        Server(vedtaksvurderingService).run()

        RapidApplication.create(env)
            .also { LagreAvkorting(it, vedtaksvurderingService) }
            .start()
        RapidApplication.create(env)
            .also { LagreVilkaarsresultat(it, vedtaksvurderingService) }
            .start()
        RapidApplication.create(env)
            .also { LagreBeregningsresultat(it, vedtaksvurderingService) }
            .start()



    }


}



