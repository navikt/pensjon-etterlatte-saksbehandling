package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandling(
    val id: UUID,
    val sak: Long,
    val sakType: SakType,
    val behandlingOpprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val soeknadMottattDato: LocalDateTime?,
    val innsender: String?,
    val soeker: String,
    val gjenlevende: List<String>?,
    val avdoed: List<String>?,
    val soesken: List<String>?,
    val gyldighetsproeving: GyldighetsResultat?,
    val status: BehandlingStatus,
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?,
    val utenlandstilsnitt: Utenlandstilsnitt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val kommerBarnetTilgode: KommerBarnetTilgode?,
    val revurderingsaarsak: RevurderingAarsak? = null,
    val prosesstype: Prosesstype
)