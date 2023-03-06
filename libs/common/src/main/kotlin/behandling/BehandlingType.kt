package no.nav.etterlatte.libs.common.behandling

enum class BehandlingType {
    FØRSTEGANGSBEHANDLING, REVURDERING, MANUELT_OPPHOER, OMREGNING; // ktlint-disable

    fun trengerIkkeVilkaarsvurdering() = this == MANUELT_OPPHOER || this == OMREGNING
}