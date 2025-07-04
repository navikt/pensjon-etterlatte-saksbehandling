package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utlandstilknytning
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering

data class SakMedUtlandstilknytning(
    val ident: String,
    val sakType: SakType,
    val id: SakId,
    val enhet: Enhetsnummer,
    val adressebeskyttelse: AdressebeskyttelseGradering?,
    val erSkjermet: Boolean?,
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
            adressebeskyttelse = sak.adressebeskyttelse,
            erSkjermet = sak.erSkjermet,
            utlandstilknytning = utlandstilknytning,
        )
    }
}
