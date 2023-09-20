package no.nav.etterlatte.fordeler

import io.prometheus.client.Counter
import no.nav.etterlatte.fordeler.FordelerResultat.IkkeGyldigForBehandling

private val defaultKriterierIkkeOppfyltMetric =
    Counter.build(
        "etterlatte_fordeler_kriterier_ikke_oppfylt_total",
        "Søknad avvist i fordeler fordi ett eller flere kriterier ikke er oppfylt",
    ).labelNames("kriterie").create()

private val defaultFordelerStatusMetric =
    Counter.build(
        "etterlatte_fordeler_soknad_fordelt_total",
        "Viser om en søknad er gyldig for behandling eller ikke",
    ).labelNames("fordelt").create()

enum class FordelerStatus {
    GYLDIG_FOR_BEHANDLING,
    IKKE_GYLDIG_FOR_BEHANDLING,
}

internal class FordelerMetricLogger(
    private val kriterierIkkeOppfyltMetric: Counter = defaultKriterierIkkeOppfyltMetric.register(),
    private val fordelerStatusMetric: Counter = defaultFordelerStatusMetric.register(),
) {
    fun logMetricFordelt() {
        this.fordelerStatusMetric.labels(FordelerStatus.GYLDIG_FOR_BEHANDLING.name).inc()
    }

    fun logMetricIkkeFordelt(resultat: IkkeGyldigForBehandling) {
        resultat.ikkeOppfylteKriterier.forEach {
            kriterierIkkeOppfyltMetric.labels(it.name).inc()
        }
        fordelerStatusMetric.labels(FordelerStatus.IKKE_GYLDIG_FOR_BEHANDLING.name).inc()
    }
}
