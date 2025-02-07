package sak

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Flyktning
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt

data class KomplettSak(
    val id: SakId,
    val ident: String,
    val sakType: SakType,
    val adressebeskyttelse: AdressebeskyttelseGradering?,
    val erSkjermet: Boolean?,
    val enhet: Enhetsnummer,
    val flyktning: Flyktning?,
    // TODO: Denne burde være non-nullable, men det krever opprydding i databasen
    val opprettet: Tidspunkt?,
)
