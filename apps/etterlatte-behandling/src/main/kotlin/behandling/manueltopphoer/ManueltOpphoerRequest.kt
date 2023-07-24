package no.nav.etterlatte.behandling.manueltopphoer

data class ManueltOpphoerRequest(
    val sakId: Long,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?
)