package no.nav.etterlatte.statistikk.service

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.statistikk.database.SoeknadStatistikkRepository
import no.nav.etterlatte.statistikk.domain.SoeknadStatistikk

interface SoeknadStatistikkService {

    fun registrerSoeknadStatistikk(
        soeknadId: Long,
        gyldigForBehandling: Boolean,
        sakType: SakType,
        feilendeKriterier: List<String>?
    ): SoeknadStatistikk
}

class SoeknadStatistikkServiceImpl(
    private val soeknadStatistikkRepository: SoeknadStatistikkRepository
) : SoeknadStatistikkService {

    override fun registrerSoeknadStatistikk(
        soeknadId: Long,
        gyldigForBehandling: Boolean,
        sakType: SakType,
        feilendeKriterier: List<String>?
    ): SoeknadStatistikk {
        val statistikk = SoeknadStatistikk(
            soeknadId = soeknadId,
            gyldigForBehandling = gyldigForBehandling,
            sakType = sakType,
            kriterierForIngenBehandling = feilendeKriterier ?: emptyList()
        )

        return soeknadStatistikkRepository.lagreNedSoeknadStatistikk(statistikk)
    }
}