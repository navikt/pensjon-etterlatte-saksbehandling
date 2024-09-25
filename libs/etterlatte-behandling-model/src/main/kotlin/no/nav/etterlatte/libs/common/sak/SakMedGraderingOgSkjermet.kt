package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering

data class SakMedGraderingOgSkjermet(
    val id: SakId,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering?,
    val erSkjermet: Boolean?,
    val enhetNr: Enhetsnummer?,
)
