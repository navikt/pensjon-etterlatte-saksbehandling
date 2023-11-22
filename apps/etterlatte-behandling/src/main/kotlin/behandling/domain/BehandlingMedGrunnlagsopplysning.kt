package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.sak.SakUtenlandstilknytning
import java.time.LocalDateTime

data class BehandlingMedGrunnlagsopplysning<Person>(
    val behandling: Behandling,
    val soeknadMottattDato: LocalDateTime?,
    val personopplysning: Grunnlagsopplysning<Person>?,
    val utenlandstilknytning: SakUtenlandstilknytning?,
)
