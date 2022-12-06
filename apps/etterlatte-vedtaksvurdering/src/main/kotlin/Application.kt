package no.nav.etterlatte // ktlint-disable filename

import no.nav.etterlatte.vedtaksvurdering.database.DataSourceBuilder
import no.nav.etterlatte.vedtaksvurdering.database.VedtaksvurderingRepository
import no.nav.etterlatte.vedtaksvurdering.rivers.AttesterVedtak
import no.nav.etterlatte.vedtaksvurdering.rivers.FattVedtak
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreBeregningsresultat
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreIverksattVedtak
import no.nav.etterlatte.vedtaksvurdering.rivers.LagreVilkaarsresultat
import no.nav.etterlatte.vedtaksvurdering.rivers.UnderkjennVedtak
import no.nav.helse.rapids_rivers.RapidApplication
import rapidsandrivers.vedlikehold.registrerVedlikeholdsriver

fun main() {
    app()
}

fun app(env: Map<String, String> = System.getenv()) {
    val localDev = env["LOCAL_DEV"].toBoolean()
    val ds = DataSourceBuilder(env)
    ds.migrate()

    val vedtakRepo = VedtaksvurderingRepository.using(ds.dataSource)
    val vedtaksvurderingService = VedtaksvurderingService(vedtakRepo)

    RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(
            env.toMutableMap().apply {
                put("KAFKA_CONSUMER_GROUP_ID", get("NAIS_APP_NAME")!!.replace("-", ""))
            }
        )
    ).withKtorModule {
        module(vedtaksvurderingService, localDev)
    }.build().apply {
        LagreVilkaarsresultat(this, vedtaksvurderingService)
        LagreBeregningsresultat(this, vedtaksvurderingService)
        FattVedtak(this, vedtaksvurderingService)
        AttesterVedtak(this, vedtaksvurderingService)
        UnderkjennVedtak(this, vedtaksvurderingService)
        LagreIverksattVedtak(this, vedtaksvurderingService)
        registrerVedlikeholdsriver(vedtaksvurderingService)
    }.start()
}