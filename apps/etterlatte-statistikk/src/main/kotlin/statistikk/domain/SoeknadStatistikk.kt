package no.nav.etterlatte.statistikk.domain

import no.nav.etterlatte.libs.common.behandling.SakType

data class SoeknadStatistikk(
    val soeknadId: Long,
    val gyldigForBehandling: Boolean,
    val sakType: SakType,
    val kriterierForIngenBehandling: List<String>
) {
    init {
        val erKonsekvent = when (gyldigForBehandling) {
            true -> kriterierForIngenBehandling.isEmpty()
            false -> kriterierForIngenBehandling.isNotEmpty()
        }
        require(erKonsekvent) {
            "En søknad skal enten være gyldig for behandling med ingen kriterier for ingen behandling, " +
                "eller den er ikke gyldig for behandling på grunn av en eller flere kriterier "
        }
    }
}