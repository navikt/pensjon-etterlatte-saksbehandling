package no.nav.etterlatte.libs.common.sak

import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering

data class Sak(
    val ident: String,
    val sakType: SakType,
    val id: SakId,
    val enhet: Enhetsnummer,
    val adressebeskyttelse: AdressebeskyttelseGradering?,
    val erSkjermet: Boolean?,
)

data class VedtakSak(
    val ident: String,
    val sakType: SakType,
    val id: SakId,
)

data class SakslisteDTO(
    val sakIdListe: List<SakId>,
)
