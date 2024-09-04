package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning

data class SakMedUtlandstilknytning(
    val ident: String,
    val sakType: SakType,
    val id: Long,
    val enhet: String,
    val utlandstilknytning: Utlandstilknytning?,
) {
    companion object {
        fun fra(
            sak: Sak,
            utlandstilknytning: Utlandstilknytning?,
        ) = SakMedUtlandstilknytning(
            ident = sak.ident,
            sakType = sak.sakType,
            id = sak.id,
            enhet = sak.enhet,
            utlandstilknytning = utlandstilknytning,
        )
    }
}
