package no.nav.etterlatte // ktlint-disable filename

import no.nav.etterlatte.database.DataSourceBuilder
import no.nav.etterlatte.database.VedtaksvurderingRepository
import no.nav.etterlatte.rivers.AttesterVedtak
import no.nav.etterlatte.rivers.FattVedtak
import no.nav.etterlatte.rivers.LagreAvkorting
import no.nav.etterlatte.rivers.LagreBeregningsresultat
import no.nav.etterlatte.rivers.LagreIverksattVedtak
import no.nav.etterlatte.rivers.LagreKommerSoekerTilgodeResultat
import no.nav.etterlatte.rivers.LagreVilkaarsresultat
import no.nav.etterlatte.rivers.UnderkjennVedtak
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver

fun main() {
    val ds = DataSourceBuilder(System.getenv())
    ds.migrate()

    val vedtakRepo = VedtaksvurderingRepository.using(ds.dataSource)

    val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)

    System.getenv().toMutableMap().apply {
        put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
    }.also { env ->

        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(env)).withKtorModule {
            module(vedtaksvurderingService)
        }.build().apply {
            LagreAvkorting(this, vedtaksvurderingService)
            LagreVilkaarsresultat(this, vedtaksvurderingService)
            LagreBeregningsresultat(this, vedtaksvurderingService)
            LagreKommerSoekerTilgodeResultat(this, vedtaksvurderingService)
            FattVedtak(this, vedtaksvurderingService)
            AttesterVedtak(this, vedtaksvurderingService)
            UnderkjennVedtak(this, vedtaksvurderingService)
            LagreIverksattVedtak(this, vedtaksvurderingService)
            registrerVedlikeholdsriver(vedtaksvurderingService)
        }.start()
    }
}