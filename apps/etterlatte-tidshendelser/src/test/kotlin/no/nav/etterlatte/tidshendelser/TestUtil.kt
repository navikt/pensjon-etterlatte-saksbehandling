package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId

fun sak(
    id: SakId,
    sakType: SakType,
    ident: String = "123",
    enhet: Enhetsnummer = Enheter.PORSGRUNN.enhetNr,
    adressebeskyttelse: AdressebeskyttelseGradering? = null,
    erSkjermet: Boolean? = null,
): Sak =
    Sak(
        ident = ident,
        sakType = sakType,
        id = id,
        enhet = enhet,
        adressebeskyttelse = adressebeskyttelse,
        erSkjermet = erSkjermet,
    )
