package no.nav.etterlatte.vedtaksvurdering

import no.nav.etterlatte.libs.common.behandling.BehandlingType

enum class TolketVedtak {
    INNVILGET,
    OPPHOER
}

/**
 * TODO ai 10.02.2023: Se på denne logikken og fiks tolking av vedtak
 * */
internal fun Vedtak.tolkVedtak(): TolketVedtak = when (this.behandlingType) {
    BehandlingType.FØRSTEGANGSBEHANDLING -> TolketVedtak.INNVILGET
    BehandlingType.REGULERING -> TolketVedtak.INNVILGET
    BehandlingType.REVURDERING -> TolketVedtak.OPPHOER
    BehandlingType.MANUELT_OPPHOER -> TolketVedtak.OPPHOER
}