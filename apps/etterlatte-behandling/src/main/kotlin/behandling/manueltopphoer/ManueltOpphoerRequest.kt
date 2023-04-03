package no.nav.etterlatte.behandling.manueltopphoer

data class ManueltOpphoerRequest(
    val sak: Long,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?
)