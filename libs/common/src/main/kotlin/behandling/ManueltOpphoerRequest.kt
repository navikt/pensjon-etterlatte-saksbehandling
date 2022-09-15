package no.nav.etterlatte.libs.common.behandling

data class ManueltOpphoerRequest(
    val sak: Long,
    val opphoerAarsaker: List<ManueltOpphoerAarsak>,
    val fritekstAarsak: String?
)