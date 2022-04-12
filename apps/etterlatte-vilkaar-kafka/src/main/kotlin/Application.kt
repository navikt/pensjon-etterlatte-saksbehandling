package no.nav.etterlatte

import BehandlingOpprettet
import BehovBesvart
import DataSourceBuilder
import LesVilkaarsmelding
import no.nav.etterlatte.model.VilkaarService
import no.nav.helse.rapids_rivers.RapidApplication
import vilkaar.Dao
import vilkaar.VurderVilkaarImpl
import vilkaar.VurderteVilkaarDao

fun main() {

    val datasource = DataSourceBuilder(System.getenv()).also { it.migrate() }.dataSource

    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
        //TODO refaktorere ut appbuilder
    }.also { env ->
        RapidApplication.create(env)
            .also {
                LesVilkaarsmelding(it, VilkaarService())
                VurderVilkaarImpl(Dao(datasource, ::VurderteVilkaarDao)).also { svc ->
                    BehandlingOpprettet(it, svc)
                    BehovBesvart(it, svc)
                }
            }.start()
    }
}


