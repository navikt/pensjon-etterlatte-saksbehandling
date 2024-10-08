package no.nav.etterlatte.tidshendelser

import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId

fun sak(
    id: SakId,
    sakType: SakType,
    ident: String = "123",
    enhet: Enhetsnummer = Enheter.PORSGRUNN.enhetNr,
): Sak =
    Sak(
        ident = ident,
        sakType = sakType,
        id = id,
        enhet = enhet,
    )
